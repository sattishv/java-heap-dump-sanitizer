/*-
 * ​​​
 * java-heap-dump-sanitizer
 * ⁣⁣⁣
 * Copyright (C) 2020 the original author or authors.
 * ⁣⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ﻿﻿﻿﻿﻿
 */

package com.github.java.heap.dump.sanitizer;

import com.github.java.heap.dump.sanitizer.SanitizeCommand.ManifestVersionProvider;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.text.StringEscapeUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

@Command(description = "Sanitize a Java heap dump by replacing byte or char array contents",
        name = "sanitize",
        usageHelpAutoWidth = true,
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class)
public class SanitizeCommand implements Callable<Void> {

    static CommandLine commandLine = newCommandLine();

    @Parameters(index = "0", description = "Input heap dump .hprof. File or stdin")
    private File inputFile;

    @Option(names = { "-a", "--tar-input" }, description = "Indicates that input is a tar archive")
    private boolean tarInput;

    @Parameters(index = "1", description = "Output heap dump .hprof. File, stdout, or stderr")
    private File outputFile;

    @Option(names = { "-z", "--zip-output" }, description = "Indicates that output should be zipped")
    private boolean zipOutput;

    @Option(names = { "-t", "--text" }, description = "Sanitization text to replace with", defaultValue = "\\0",
            showDefaultValue = ALWAYS)
    private String sanitizationText;

    @Option(names = { "-b", "--buffer-size" }, description = "Buffer size for reading and writing",
            defaultValue = "100MB", showDefaultValue = ALWAYS)
    private DataSize bufferSize = DataSize.ofMegabytes(100);

    public static void main(String... args) throws Exception {
        commandLine.registerConverter(DataSize.class, DataSize::parse);
        commandLine.execute(args);
    }

    static CommandLine newCommandLine() {
        return new CommandLine(new SanitizeCommand());
    }

    @Override
    public Void call() throws Exception {
        newRunner().run();
        return null;
    }

    protected SanitizeCommandRunner newRunner() {
        return new SanitizeCommandRunner(this);
    }

    public File getInputFile() {
        return inputFile;
    }

    public boolean isTarInput() {
        return tarInput;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean isZipOutput() {
        return zipOutput;
    }

    public String getSanitizationText() {
        return StringEscapeUtils.unescapeJava(sanitizationText);
    }

    public int getBufferByteSize() {
        return Math.toIntExact(bufferSize.toBytes());
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public void setTarInput(boolean tarInput) {
        this.tarInput = tarInput;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setZipOutput(boolean zipOutput) {
        this.zipOutput = zipOutput;
    }

    public void setSanitizationText(String sanitizationText) {
        this.sanitizationText = sanitizationText;
    }

    public void setBufferByteSize(int bytes) {
        bufferSize = DataSize.ofBytes(bytes);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    static class ManifestVersionProvider implements IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            String version = getClass().getPackage().getImplementationVersion();
            return new String[] { version != null ? version : "unknown" };
        }

    }
}
