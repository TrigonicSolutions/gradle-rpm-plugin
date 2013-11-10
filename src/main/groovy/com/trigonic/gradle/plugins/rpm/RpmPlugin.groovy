/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trigonic.gradle.plugins.rpm

import org.freecompany.redline.Builder
import org.freecompany.redline.header.Architecture
import org.freecompany.redline.header.Flags
import org.freecompany.redline.header.Os
import org.freecompany.redline.header.RpmType
import org.freecompany.redline.payload.Directive
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.plugins.BasePlugin

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class RpmPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply(BasePlugin.class)

        project.ext.Rpm = Rpm.class

        CopySpecImpl.metaClass.user = null
        CopySpecImpl.metaClass.group = null
        CopySpecImpl.metaClass.fileType = null
        CopySpecImpl.metaClass.createDirectoryEntry = null
        CopySpecImpl.metaClass.addParentDirs = true

        Field.metaClass.hasModifier = { modifier ->
            (modifiers & modifier) == modifier
        }

        Builder.metaClass.getDefaultSourcePackage() {
            format.getLead().getName() + "-src.rpm"
        }

        Directive.metaClass.or = { Directive other ->
            new Directive(delegate.flag | other.flag)
        }

        CopySpecImpl.WrapperCopySpec.metaClass.getFileType= {->
            return spec.fileType;
        }

        CopySpecImpl.WrapperCopySpec.metaClass.getUser= {->
            return spec.user;
        }

        CopySpecImpl.WrapperCopySpec.metaClass.getAddParentDirs= {->
            return spec.addParentDirs;
        }

        CopySpecImpl.WrapperCopySpec.metaClass.getCreateDirectoryEntry= {->
            return spec.createDirectoryEntry;
        }

        CopySpecImpl.WrapperCopySpec.metaClass.getGroup= {->
            return spec.group;
        }

        aliasEnumValues(Architecture.values())
        aliasEnumValues(Os.values())
        aliasEnumValues(RpmType.values())
        aliasStaticInstances(Directive.class)
        aliasStaticInstances(Flags.class, int.class)

    }


    private <T extends Enum<T>> void aliasEnumValues(T[] values) {
        for (T value : values) {
            assert !CopySpecImpl.hasProperty(value.name())
            CopySpecImpl.metaClass."${value.name()}" = value
        }
    }

    private <T> void aliasStaticInstances(Class<T> forClass) {
        aliasStaticInstances forClass, forClass
    }

    private <T, U> void aliasStaticInstances(Class<T> forClass, Class<U> ofClass) {
        for (Field field : forClass.fields) {
            if (field.type == ofClass && field.hasModifier(Modifier.STATIC)) {
                assert !CopySpecImpl.metaClass.hasProperty(field.name)
                CopySpecImpl.metaClass."${field.name}" = field.get(null)
            }
        }
    }

}
