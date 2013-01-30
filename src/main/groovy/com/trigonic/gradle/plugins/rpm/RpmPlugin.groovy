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

import java.lang.reflect.Field

import org.freecompany.redline.Builder
import org.freecompany.redline.payload.Directive
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.copy.CopySpecImpl
import org.gradle.api.plugins.BasePlugin

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
    }
}