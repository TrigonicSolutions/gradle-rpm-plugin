/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trigonic.gradle.plugins.rpm

import org.gradle.api.tasks.bundling.Tar

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import org.apache.commons.io.FileUtils
import static org.freecompany.redline.payload.CpioHeader.*
import static org.freecompany.redline.header.Header.HeaderTag.*

import org.freecompany.redline.header.Header.HeaderTag
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.gmock.GMockController

class RpmPluginTest {
    @Test
    public void files() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(buildDir, 'noParentsDir')
        noParentsDir.mkdirs()
        FileUtils.writeStringToFile(new File(noParentsDir, 'alone'), 'alone')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386
            os = LINUX
            group = 'Development/Libraries'
            summary = 'Bleah blarg'
            description = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'
            
            requires('blarg', '1.0', GREATER | EQUAL)
            requires('blech')

            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                createDirectoryEntry = true
                fileType = CONFIG | NOREPLACE
            }
            
            from(noParentsDir) {
                addParentDirs = false
                into '/a/path/not/to/create'
            }

            link('/opt/bleah/banana', '/opt/bleah/apple')
        })

        project.tasks.buildRpm.execute()
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        assertEquals('bleah', getHeaderEntryString(scan, NAME))
        assertEquals('1.0', getHeaderEntryString(scan, VERSION))
        assertEquals('1', getHeaderEntryString(scan, RELEASE))
        assertEquals('i386', getHeaderEntryString(scan, ARCH))
        assertEquals('linux', getHeaderEntryString(scan, OS))
        assertEquals(['./a/path/not/to/create/alone', './opt/bleah',
                      './opt/bleah/apple', './opt/bleah/banana'], scan.files*.name)
        assertEquals([FILE, DIR, FILE, SYMLINK], scan.files*.type)
    }


    @Test
    public void filesWithCopySpec() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(buildDir, 'noParentsDir')
        noParentsDir.mkdirs()
        FileUtils.writeStringToFile(new File(noParentsDir, 'alone'), 'alone')

        project.apply plugin: 'rpm'

        def rpmCopySpec = project.copySpec {
            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                fileMode = 0644
                createDirectoryEntry = true
                fileType = CONFIG | NOREPLACE
            }

            from(noParentsDir) {
                fileMode = 0755
                addParentDirs = false
                into '/a/path/not/to/create'
            }
        }

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386
            os = LINUX
            group = 'Development/Libraries'
            summary = 'Bleah blarg'
            description = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            with rpmCopySpec
            link('/opt/bleah/banana', '/opt/bleah/apple')

        })

        project.tasks.buildRpm.execute()
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        assertEquals('bleah', getHeaderEntryString(scan, NAME))
        assertEquals('1.0', getHeaderEntryString(scan, VERSION))
        assertEquals('1', getHeaderEntryString(scan, RELEASE))
        assertEquals('i386', getHeaderEntryString(scan, ARCH))
        assertEquals('linux', getHeaderEntryString(scan, OS))
        assertEquals(['./a/path/not/to/create/alone', './opt/bleah',
                './opt/bleah/apple', './opt/bleah/banana'], scan.files*.name)
        assertEquals([FILE, DIR, FILE, SYMLINK], scan.files*.type)

        project.apply plugin: 'java'
        project.task([type: Tar], 'buildJar', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            with rpmCopySpec
        })
        project.tasks.buildJar.execute()
    }

    @Test
    public void projectNameDefault() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})
        assertEquals 'test', project.buildRpm.packageName

        project.tasks.buildRpm.execute()
    }

    @Test
    void buildHost_shouldHaveASensibleDefault_whenHostNameResolutionFails() {
        GMockController mock = new GMockController()
        InetAddress mockInetAddress = (InetAddress) mock.mock(InetAddress)

        mockInetAddress.static.getLocalHost().raises(new UnknownHostException())

        mock.play {
            Project project = ProjectBuilder.builder().build()

            File buildDir = project.buildDir
            File srcDir = new File(buildDir, 'src')
            srcDir.mkdirs()
            FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

            project.apply plugin: 'rpm'

            project.task([type: Rpm], 'buildRpm', {})
            assertEquals 'unknown', project.buildRpm.buildHost

            project.tasks.buildRpm.execute()
        }

    }

    @Test
    public void usesArchivesBaseName() {
        Project project = ProjectBuilder.builder().build()
        project.archivesBaseName = 'foo'

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})
        assertEquals 'foo', project.buildRpm.packageName

        project.tasks.buildRpm.execute()
    }
    
    def getHeaderEntry = { scan, tag ->
        def header = scan.format.header
        header.getEntry(tag.code)
    }
    
    def getHeaderEntryString = { scan, tag ->
        getHeaderEntry(scan, tag).values.join('')
    }

    @Test
    public void differentUsersBetweenCopySpecs() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir1 = new File(buildDir, 'src1')
        File srcDir2 = new File(buildDir, 'src2')
        File srcDir3 = new File(buildDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            user = 'default'

            from(srcDir1) {
                user = 'user1'
            }

            from(srcDir2) {
                // should be default user
            }

            from(srcDir3) {
                user = 'user2'
            }
        })

        project.tasks.buildRpm.execute()

        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        assertEquals([DIR, FILE, FILE, FILE], scan.files*.type)
        assertEquals(['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'], scan.files*.name)

        assertEquals(['user1', 'user1', 'default', 'user2'],
                     scan.format.header.getEntry(HeaderTag.FILEUSERNAME).values.toList())
    }

    @Test
    public void differentGroupsBetweenCopySpecs() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir1 = new File(buildDir, 'src1')
        File srcDir2 = new File(buildDir, 'src2')
        File srcDir3 = new File(buildDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            group = 'default'

            from(srcDir1) {
                // should be default group
            }

            from(srcDir2) {
                group = 'group2'
            }

            from(srcDir3) {
                // should be default group
            }
        })

        project.tasks.buildRpm.execute()

        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        assertEquals([DIR, FILE, FILE, FILE], scan.files*.type)
        assertEquals(['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'], scan.files*.name)

        assertEquals(['default', 'default', 'group2', 'default'],
                     scan.format.header.getEntry(HeaderTag.FILEGROUPNAME).values.toList())
    }

    @Test
    public void differentPermissionsBetweenCopySpecs() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir1 = new File(buildDir, 'src1')
        File srcDir2 = new File(buildDir, 'src2')
        File srcDir3 = new File(buildDir, 'src3')

        srcDir1.mkdirs()
        srcDir2.mkdirs()
        srcDir3.mkdirs()

        FileUtils.writeStringToFile(new File(srcDir1, 'apple'),  'apple')
        FileUtils.writeStringToFile(new File(srcDir2, 'banana'), 'banana')
        FileUtils.writeStringToFile(new File(srcDir3, 'cherry'), 'cherry')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'userTest'
            version     = '2.0'
            release     = '2'
            type        = BINARY
            arch        = I386
            os          = LINUX

            into '/tiny'
            fileMode = 0555

            from(srcDir1) {
                // should be default group
            }

            from(srcDir2) {
                fileMode = 0666
            }

	    from(srcDir3) {
		fileMode = 0555
            }
	})

        project.tasks.buildRpm.execute()

        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/userTest-2.0-2.i386.rpm'))

        assertEquals([DIR, FILE, FILE, FILE], scan.files*.type)
        assertEquals(['./tiny', './tiny/apple', './tiny/banana', './tiny/cherry'], scan.files*.name)

        // #define S_IFIFO  0010000  /* named pipe (fifo) */
        // #define S_IFCHR  0020000  /* character special */
        // #define S_IFDIR  0040000  /* directory */
        // #define S_IFBLK  0060000  /* block special */
        // #define S_IFREG  0100000  /* regular */
        // #define S_IFLNK  0120000  /* symbolic link */
        // #define S_IFSOCK 0140000  /* socket */
        // #define S_ISUID  0004000 /* set user id on execution */
        // #define S_ISGID  0002000 /* set group id on execution */
        // #define S_ISTXT  0001000 /* sticky bit */
        // #define S_IRWXU  0000700 /* RWX mask for owner */
        // #define S_IRUSR  0000400 /* R for owner */
        // #define S_IWUSR  0000200 /* W for owner */
        // #define S_IXUSR  0000100 /* X for owner */
        // #define S_IRWXG  0000070 /* RWX mask for group */
        // #define S_IRGRP  0000040 /* R for group */
        // #define S_IWGRP  0000020 /* W for group */
        // #define S_IXGRP  0000010 /* X for group */
        // #define S_IRWXO  0000007 /* RWX mask for other */
        // #define S_IROTH  0000004 /* R for other */
        // #define S_IWOTH  0000002 /* W for other */
        // #define S_IXOTH  0000001 /* X for other */
        // #define S_ISVTX  0001000 /* save swapped text even after use */

	// drwxr-xr-x is 0040755
        // NOTE: Not sure why directory is getting user write permission
	assertEquals([(short)0040755, (short)0100555, (short)0100666, (short)0100555],
                     scan.format.header.getEntry(HeaderTag.FILEMODES).values.toList())
    }
}
