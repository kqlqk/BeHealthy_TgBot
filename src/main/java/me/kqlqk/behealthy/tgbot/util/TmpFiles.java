package me.kqlqk.behealthy.tgbot.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class TmpFiles {
    @Value("${files.tmp.dir}")
    private String dir;

    public void cleanByName(String name) throws IOException {
        new File(dir + "/" + name).delete();
    }
}
