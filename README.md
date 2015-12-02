# Android-Uninstall-Feedback

## Introduction

When your app is uninstalled, you can start a intent or a web browser.

## Installation

Take from maven repository (<http://search.maven.org/#search%7Cga%7C1%7Ccn.hiroz>, <http://mvnrepository.com/search.html?query=cn.hiroz>) or add Coredroid and other components to your solution


Add it as dependency in Gradle as:

```
compile 'cn.hiroz:uninstallfeedback-lib:1.2.2@aar'
```

## ProGuard

You may add this line:

```
-keep class cn.hiroz.uninstallfeedback.** { *; }
```

## Usage

Add the following code to the initialization of app. For example, `OnCreate` method in Application or Activity. 

**Note:** Keep this code is only one. It will fork a thread. 

```
FeedbackUtils.openUrlWhenUninstall(this, "www.baidu.com");
```

Run your app, uninstall it. Then the browser startup.

## Who Use It

![360zhushou](http://p19.qhimg.com/t01b792441769dbca78.png) [360手机助手](http://sj.360.cn/): The largest mobile app distribution platform in China. It has more than 5 billions of users.

## Copyrights

   The MIT License (MIT)

   Copyright (c) 2015 Hiro (hiroz.cn)

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all
   copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   SOFTWARE.
