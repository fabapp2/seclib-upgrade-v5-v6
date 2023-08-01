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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.sbm.engine.commands.ApplyCommand;
import org.springframework.sbm.engine.commands.ScanCommand;
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.sbm.engine.context.ProjectContextSerializer;
import org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter;
import org.springframework.sbm.engine.recipe.RewriteRecipeLoader;
import org.springframework.sbm.engine.recipe.RewriteRecipeRunner;
import org.springframework.sbm.project.parser.PathScanner;
import org.springframework.sbm.project.parser.ProjectContextInitializer;
import org.springframework.sbm.project.resource.BaseProjectResource;
import org.springframework.sbm.project.resource.RewriteSourceFileHolder;
import org.springframework.sbm.support.openrewrite.GenericOpenRewriteRecipe;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Fabian Krüger
 */
@SpringBootTest(classes = MigratorApp.class, properties = "sbm.gitSupportEnabled=true")
@ActiveProfiles("test")
public class Migrate5to6RecipeTest {

    public static final String PROJECT_ROOT = "/Users/fkrueger/demo-nt/demo-2/business-service";

    @Nested
    class UsingComponents {
        @Autowired
        private ProjectContextInitializer projectContextInitializer;
        @Autowired
        private PathScanner scanner;
        @Autowired
        private RewriteRecipeLoader rewriteRecipeLoader;
        @Autowired
        private RewriteRecipeRunner recipeRunner;
        @Autowired
        private ProjectContextSerializer contextSerializer;

        @Test
        @DisplayName("testIt")
        void testIt() {
            // Path to the project to migrate
            Path baseDir = Path.of(PROJECT_ROOT);

            // Scan project
            List<Resource> resources = scanner.scan(baseDir);

            // Create ProjectContext abstraction
            ProjectContext pc = projectContextInitializer.initProjectContext(baseDir, resources);

            // Applying an inline declarative YAML recipe in plain OpenRewrite syntax
            String s = """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: com.acme.migration.UpdateSecLib5To6
                    displayName: Upgrade SecLib from v5 to v6
                    description: 'Upgrades SecLib from v5 to v6.'
                    recipeList:
                      - org.openrewrite.maven.UpgradeDependencyVersion:
                          groupId: com.acme.seclib
                          artifactId: seclib-core
                          newVersion: 6.0.0                
                    """;
            // Using an adapter action
            OpenRewriteDeclarativeRecipeAdapter recipeAdapter = new OpenRewriteDeclarativeRecipeAdapter(s, rewriteRecipeLoader, recipeRunner);
            recipeAdapter.apply(pc);

            // write back changes
            contextSerializer.writeChanges(pc);

            // parse again to resolve new types (from v6)
            resources = scanner.scan(baseDir);
            pc = projectContextInitializer.initProjectContext(baseDir, resources);

            // Migrate the deprecated code
            Recipe migrateRecipe = new GenericOpenRewriteRecipe<>(() -> new MigrateToAnnotationVisitor());
            pc.getProjectJavaSources().apply(migrateRecipe);

            // print changed resources (not written to FS)
            pc.getProjectResources()
                    .streamIncludingDeleted()
                    .filter(BaseProjectResource::hasChanges)
                    .map(RewriteSourceFileHolder::print)
                    .forEach(System.out::println);
        }
    }


    @Nested
    class UsingCommands {

        @Autowired
        ScanCommand scanCommand;

        @Autowired
        ApplyCommand applyCommand;

        @Autowired
        @Qualifier("secLib5to6MigrationRecipe")
        org.springframework.sbm.engine.recipe.Recipe secLib5to6MigrationRecipe;

        @Test
        @DisplayName("using commands")
        @Disabled("Use manually as it migrates the business-service")
        void usingCommands() {
            ProjectContext pc = scanCommand.execute(PROJECT_ROOT);
            applyCommand.execute(pc, "migrate-seclib-5-to-6");
        }

    }
}
