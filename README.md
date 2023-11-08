# <h1 align="center"><img src="./arts/logo.svg" style="width:140px;height:100px;"/><br>PeekAlert</h1>

<p align="center">
A Lightweight and Highly-customizable alert library
</p>

<p align="center">
    <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"></a>
    <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white"></a>
    <a href="./"><img src="https://img.shields.io/github/repo-size/mooner1022/PeekAlert?&style=for-the-badge"></a>
    <a href="./LICENSE"><img src="https://img.shields.io/github/license/mooner1022/PeekAlert?&style=for-the-badge"></a>
</p>

<p align="center">
PeekAlert can be used on both Java and Kotlin.<br>
See the examples below for more info.
</p>

<p align="center">
    ✔ Any PRs are welcome!
</p>

### Demo
<p align="center">
    <img src="./arts/demo_1.png" width="24%">
    <img src="./arts/demo_2.png" width="24%">
    <img src="./arts/demo_3.png" width="24%">
</p>

### Installation (Gradle)
+ Add jitpack url

Kotlin DSL:
```kotlin
repositories {
    /* ... */
    maven(url = "https://jitpack.io")
}
```

Groovy DSL:
```groovy
repositories {
    /* ... */
    maven { url 'https://jitpack.io' }
}
```

+ Add PeekAlert dependency  
[![](https://jitpack.io/v/mooner1022/PeekAlert.svg)](https://jitpack.io/#mooner1022/PeekAlert)
```kotlin
dependencies {
    implementation("com.github.mooner1022:PeekAlert:<VERSION>")
}
```

### Usage
#### Basic usages

Java
```java
PeekAlert peekAlert = PeekAlert.create(this)
                .setDraggable(true)
                .setTitle("Hello, PeekAlert!")
                .setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");

peekAlert.peek();
```
Kotlin
```kotlin
val peekAlert = PeekAlert.create(this)
            .setDraggable(true)
            .setTitle("Hello, PeekAlert!")
            .setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")

peekAlert.peek()
```

Builder for kotlin is also available:
```kotlin
createPeekAlert(this) {
    draggable = true
    title("Hello, PeekAlert Builder!") {
        textColor(res = R.color.black)
    }
    text("Lorem ipsum dolor sit amet, consectetur adipiscing elit.") {
        textSize = 14f
    }
}.peek()
```

Custom
```kotlin
To be documented...
```

### Get more info at docs
[Dokka Docs](https://profile.mooner.dev/PeekAlert/-peek-alert/dev.mooner.peekalert/-peek-alert/index.html)


### License
```
MIT License

Copyright (c) 2023 Minki Moon

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
```
