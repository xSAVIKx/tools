package org.spine3.gradle.protobuf.validators;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
public class ValidatorWriter {

    private static final Pattern COMPILE = Pattern.compile("\\.");
    private static final String rootFolder = "generated/main/java/";
    private static final String javaExtension = ".java";
    private final WriterDto writerDto;

    public ValidatorWriter(WriterDto writerDto) {
        this.writerDto = writerDto;
    }

    public void write() {
        log().debug("Writing the java class under {}", writerDto.getJavaPackage());
        final String fileFolder = rootFolder + toJavaPath(writerDto.getJavaPackage());
        final String filePath = fileFolder + File.separatorChar + writerDto.getJavaClass() + javaExtension;
        final File file = new File(filePath);
        try {
            Files.createParentDirs(file);
            Files.touch(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO:2017-03-01:illiashepilov: think about moving it to utility class and replace same logic in the FailureGenPlugin.
    private static String toJavaPath(String path) {
        final String result = COMPILE.matcher(path)
                                     .replaceAll(String.valueOf(File.separatorChar));
        return result;
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ValidatorWriter.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
