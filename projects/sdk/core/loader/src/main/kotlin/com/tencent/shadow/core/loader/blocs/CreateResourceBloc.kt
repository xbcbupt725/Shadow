/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.core.loader.blocs

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import com.tencent.shadow.core.load_parameters.LoadParameters
import com.tencent.shadow.core.loader.infos.PluginParts

object CreateResourceBloc {
    fun create(loadParameters: LoadParameters, packageArchiveInfo: PackageInfo, archiveFilePath: String, hostAppContext: Context, pluginPartsMap: MutableMap<String, PluginParts>): Resources {
        val packageManager = hostAppContext.packageManager
        packageArchiveInfo.applicationInfo.publicSourceDir = archiveFilePath
        packageArchiveInfo.applicationInfo.sourceDir = archiveFilePath
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (loadParameters.dependsOn != null && loadParameters.dependsOn.size > 0) {
                packageArchiveInfo.applicationInfo.splitPublicSourceDirs = Array(loadParameters.dependsOn.size, { it -> it.toString() })
                var index = 0
                for (partkey in loadParameters.dependsOn) {
                    val pluginPart = pluginPartsMap[partkey]
                    packageArchiveInfo.applicationInfo.splitPublicSourceDirs[index] = pluginPart?.apkPath
                    index++
                }
            }

            try {
                return packageManager.getResourcesForApplication(packageArchiveInfo.applicationInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }

        } else {
            //Android 5.0 以下的版本会出现0x6f0000等奇怪资源id问题,因此使用该方法添加依赖资源包路径
            var resource: Resources = packageManager.getResourcesForApplication(packageArchiveInfo.applicationInfo)
            val assetManager: AssetManager = resource.assets
            if (loadParameters.dependsOn != null && loadParameters.dependsOn.size > 0) {
                for (partkey in loadParameters.dependsOn) {
                    AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).invoke(
                            assetManager, pluginPartsMap[partkey]?.apkPath)
                }
            }

            return resource
        }

    }
}
