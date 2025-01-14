/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.hibernate.search.util.impl.test.file.FileUtils;
import org.hibernate.search.util.impl.test.logging.Log;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

/**
 * Code originally released under ASL 2.0.
 * <p>
 * Original code:
 * <ul>
 *     <li>https://github.com/quarkusio/quarkus/blob/8d4d3459b01203d2ce35d7847874a88941960443/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/classloading/JarClassPathElement.java#L37-L56</li>
 *     <li>https://github.com/quarkusio/quarkus/blob/8d4d3459b01203d2ce35d7847874a88941960443/core/deployment/src/main/java/io/quarkus/deployment/index/IndexingUtil.java</li>
 *     <li>https://github.com/smallrye/smallrye-common/blob/main/io/src/main/java/io/smallrye/common/io/jar/JarFiles.java#L45-L60</li>
 * </ul>
 * </p>
 */
public final class JandexIndexingUtils {

	private static final String META_INF_VERSIONS = "META-INF/versions/";
	private static final DotName OBJECT = DotName.createSimple( Object.class.getName() );

	private static final int JAVA_VERSION;

	static {
		int version = 8;
		try {
			Method versionMethod = Runtime.class.getMethod( "version" );
			Object v = versionMethod.invoke( null );
			@SuppressWarnings({ "unchecked", "raw" })
			List<Integer> list = (List<Integer>) v.getClass().getMethod( "version" ).invoke( v );
			version = list.get( 0 );
		}
		catch (Exception e) {
			//version 8
		}
		JAVA_VERSION = version;
	}

	private JandexIndexingUtils() {
	}

	public static Index indexJarOrDirectory(Path path) throws IOException {
		Path tempDirectory = null;
		if ( Files.isDirectory( path ) ) {
			tempDirectory = Files.createTempDirectory( "hsearch-" );
			path = JarUtils.directoryToJar( path, tempDirectory );
		}
		try {
			try ( JarFile jarFile = new JarFile( path.toFile() ) ) {
				return indexJar( jarFile );
			}
		}
		finally {
			if ( tempDirectory != null ) {
				FileUtils.deleteRecursively( tempDirectory );
			}
		}
	}

	private static Index indexJar(JarFile file) throws IOException {
		Indexer indexer = new Indexer();
		Enumeration<JarEntry> e = file.entries();
		boolean multiRelease = isMultiRelease( file );
		while ( e.hasMoreElements() ) {
			JarEntry entry = e.nextElement();
			if ( entry.getName().endsWith( ".class" ) ) {
				if ( multiRelease && entry.getName().startsWith( META_INF_VERSIONS ) ) {
					String part = entry.getName().substring( META_INF_VERSIONS.length() );
					int slash = part.indexOf( "/" );
					if ( slash != -1 ) {
						try {
							int ver = Integer.parseInt( part.substring( 0, slash ) );
							if ( ver <= JAVA_VERSION ) {
								try ( InputStream inputStream = file.getInputStream( entry ) ) {
									indexer.index( inputStream );
								}
							}
						}
						catch (NumberFormatException ex) {
							Log.INSTANCE.debug( "Failed to parse META-INF/versions entry", ex );
						}
					}
				}
				else {
					try ( InputStream inputStream = file.getInputStream( entry ) ) {
						indexer.index( inputStream );
					}
				}
			}
		}
		return indexer.complete();
	}

	public static void indexClass(String className, Indexer indexer, IndexView quarkusIndex,
			Set<DotName> additionalIndex, ClassLoader classLoader) {
		DotName classDotName = DotName.createSimple( className );
		if ( additionalIndex.contains( classDotName ) ) {
			return;
		}
		ClassInfo classInfo = quarkusIndex.getClassByName( classDotName );
		if ( classInfo == null ) {
			Log.INSTANCE.tracef( "Index class: %s", className );
			try ( InputStream stream = readClass( classLoader, className ) ) {
				classInfo = indexer.index( stream );
				additionalIndex.add( classInfo.name() );
			}
			catch (Exception e) {
				throw new IllegalStateException( "Failed to index: " + className, e );
			}
		}
		else {
			// The class could be indexed by quarkus - we still need to distinguish framework classes
			additionalIndex.add( classDotName );
		}
		for ( DotName annotationName : classInfo.annotations().keySet() ) {
			if ( !additionalIndex.contains( annotationName ) && quarkusIndex.getClassByName(
					annotationName ) == null ) {
				try ( InputStream annotationStream = readClass( classLoader, annotationName.toString() ) ) {
					if ( annotationStream == null ) {
						Log.INSTANCE.tracef(
								"Could not index annotation: %s (missing class or dependency)", annotationName );
					}
					else {
						Log.INSTANCE.tracef( "Index annotation: %s", annotationName );
						indexer.index( annotationStream );
						additionalIndex.add( annotationName );
					}
				}
				catch (IOException e) {
					throw new IllegalStateException( "Failed to index: " + className, e );
				}
			}
		}
		if ( classInfo.superName() != null && !classInfo.superName().equals( OBJECT ) ) {
			indexClass( classInfo.superName().toString(), indexer, quarkusIndex, additionalIndex, classLoader );
		}
	}

	public static void indexClass(String className, Indexer indexer,
			IndexView quarkusIndex, Set<DotName> additionalIndex,
			ClassLoader classLoader, byte[] beanData) {
		DotName classDotName = DotName.createSimple( className );
		if ( additionalIndex.contains( classDotName ) ) {
			return;
		}
		ClassInfo classInfo = quarkusIndex.getClassByName( classDotName );
		if ( classInfo == null ) {
			Log.INSTANCE.tracef( "Index class: %s", className );
			try ( InputStream stream = new ByteArrayInputStream( beanData ) ) {
				classInfo = indexer.index( stream );
				additionalIndex.add( classInfo.name() );
			}
			catch (IOException e) {
				throw new IllegalStateException( "Failed to index: " + className, e );
			}
		}
		else {
			// The class could be indexed by quarkus - we still need to distinguish framework classes
			additionalIndex.add( classDotName );
		}
		for ( DotName annotationName : classInfo.annotations().keySet() ) {
			if ( !additionalIndex.contains( annotationName ) && quarkusIndex.getClassByName(
					annotationName ) == null ) {
				try ( InputStream annotationStream = readClass( classLoader, annotationName.toString() ) ) {
					Log.INSTANCE.tracef( "Index annotation: %s", annotationName );
					indexer.index( annotationStream );
					additionalIndex.add( annotationName );
				}
				catch (IOException e) {
					throw new IllegalStateException( "Failed to index: " + className, e );
				}
			}
		}
	}

	private static InputStream readClass(ClassLoader classLoader, String className) {
		return classLoader.getResourceAsStream( className.replace( '.', '/' ) + ".class" );
	}

	private static boolean isMultiRelease(JarFile jarFile) {
		String value = null;

		try {
			Manifest manifest = jarFile.getManifest();
			if ( manifest != null ) {
				value = manifest.getMainAttributes().getValue( "Multi-Release" );
			}
		}
		catch (IOException var3) {
			throw new UncheckedIOException( "Cannot read manifest attributes", var3 );
		}

		return Boolean.parseBoolean( value );
	}

}
