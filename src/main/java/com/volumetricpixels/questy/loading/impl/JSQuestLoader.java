/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.volumetricpixels.questy.loading.impl;

import com.volumetricpixels.questy.Quest;
import com.volumetricpixels.questy.QuestManager;
import com.volumetricpixels.questy.loading.QuestLoader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads JavaScript quests.
 */
public class JSQuestLoader implements QuestLoader {
    private static final String SCRIPT_PREFIX =
            "with (new JavaImporter(" +
                    "Packages.com.volumetricpixels.questy," +
                    "Packages.com.volumetricpixels.questy.event," +
                    "Packages.com.volumetricpixels.questy.event.quest," +
                    "Packages.com.volumetricpixels.questy.loading," +
                    "Packages.com.volumetricpixels.questy.objective)) {";
    private static final String SCRIPT_SUFFIX =
            "}";

    private final QuestManager questManager;

    public JSQuestLoader(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public Set<Quest> loadQuests(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return null;
        }

        Set<Quest> result = new HashSet<>();
        // iterate through all files in the directory which end with .js
        for (File file : directory.listFiles(fl -> endsWith(fl, ".js"))) {
            try {
                Quest q = loadQuest(new FileReader(file));
                if (q != null) {
                    result.add(q);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public synchronized Quest loadQuest(Reader reader) {
        BufferedReader bufReader;
        if (reader instanceof BufferedReader) {
            bufReader = (BufferedReader) reader;
        } else {
            bufReader = new BufferedReader(reader);
        }

        ScriptEngine nashorn = new ScriptEngineManager()
                .getEngineByName("nashorn");
        nashorn.put("questManager", questManager);

        StringBuilder script = new StringBuilder();
        try {
            String curLine;
            while ((curLine = bufReader.readLine()) != null) {
                script.append(curLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            nashorn.eval(SCRIPT_PREFIX + script.toString() + SCRIPT_SUFFIX);
            return (Quest) ((Invocable) nashorn).invokeFunction("quest");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getQuestFormat() {
        return "JavaScript";
    }
}
