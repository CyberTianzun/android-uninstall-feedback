# Android-Uninstall-Feedback

## Introduction

When your app is uninstalled, you can start a intent or a web browser.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cn.hiroz/uninstallfeedback-lib/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/cn.hiroz/uninstallfeedback-lib/)

## Installation

Take from maven repository (<http://search.maven.org/#search%7Cga%7C1%7Ccn.hiroz>, <http://mvnrepository.com/search.html?query=cn.hiroz>) or add Coredroid and other components to your solution


Add it as dependency in Gradle as:

```
compile 'cn.hiroz:uninstallfeedback-lib:0.0.1@aar'
```

## ProGuard

You may add this line:

```
-keep class cn.hiroz.uninstallfeedback.FeedbackUtils { *; }
```

## Usage

Add the following code to the initialization of app. For example, `OnCreate` method in Application or Activity. 

**Note:** Keep this code is only one. It will fork a thread. 

```
FeedbackUtils.openUrlWhenUninstall(this, "www.baidu.com");
```

Run your app, uninstall it. Then the browser startup.

## Copyrights

   Copyright 2015, Hiro Zhang (hiroz.cn)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.