# Java Heap Dump Sanitizer

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.java-heap-dump-sanitizer/java-heap-dump-sanitizer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.java-heap-dump-sanitizer/java-heap-dump-sanitizer)
![Java CI](https://github.com/java-heap-dump-sanitizer/java-heap-dump-sanitizer/workflows/Java%20CI/badge.svg)

Java Heap Dump Sanitizer is a tool for clearing sensitive data from Java heap dumps. This is done by replacing byte and
char array contents in the heap dump file with zero values. Heap dump can then be analyzed with any tool.

Typical scenario is when a head dump needs to be sanitized before it can be given to another person or moved to a different
environment. For example, a program running in production environment may contain sensitive data (passwords, credit card
data, etc) which should not be viewable when the heap dump is copied to a development environment for analysis.

## Example

```
# download jar-with-dependencies file
$ wget -O sanitizer.jar 'https://search.maven.org/remote_content?g=com.github.java-heap-dump-sanitizer&a=java-heap-dump-sanitizer&v=LATEST&c=jar-with-dependencies'

# run the tool
$ java -jar sanitizer.jar input-heap-dump.hprof output-heap-dump.hprof
```

## Usage

```
Usage: sanitize [-ahVz] [-b=<bufferSize>] [-t=<sanitizationText>] <inputFile> <outputFile>
Sanitize a Java heap dump by replacing byte or char array contents
      <inputFile>    Input heap dump .hprof. File or stdin
      <outputFile>   Output heap dump .hprof. File, stdout, or stderr
  -a, --tar-input    Indicates that input is a tar archive
  -b, --buffer-size=<bufferSize>
                     buffer size for reading and writing
                       Default: 100MB
  -h, --help         Show this help message and exit.
  -t, --text=<sanitizationText>
                     Sanitization text to replace with
                       Default: \0
  -V, --version      Print version information and exit.
  -z, --zip-output   Indicates that output should be zipped
```

Note that inputFile and outputFile arguments may be file paths or standard streams. <br>
For example, the following command reads heap dump from stdin and outputs sanitized dump to stdout:

```
$ cat input-heap-dump.hprof | java -jar sanitizer.jar stdin stdout
```


### License

This tool is released under the Apache 2.0 license.

```
Copyright 2020.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
