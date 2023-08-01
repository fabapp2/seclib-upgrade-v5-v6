/*
 * Copyright 2021 - 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.seclib.migrator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.sbm.engine.commands.ApplyCommand;
import org.springframework.sbm.engine.commands.ScanCommand;
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.stereotype.Component;


@Component
@Profile("!test")/**
 * @author Fabian Krüger
 */
public class MigrationCommandLineRunner implements CommandLineRunner {


    @Autowired
    private ScanCommand scanCommand;
    @Autowired
    private ApplyCommand applyCommand;

    @Override
    public void run(String... args) throws Exception {
        if(args.length == 0) {
            throw new IllegalArgumentException("Please provide path to application.");
        }
        ProjectContext projectContext = scanCommand.execute(args[0]);
        applyCommand.execute(projectContext, "migrate-seclib-5-to-6");
    }
}
