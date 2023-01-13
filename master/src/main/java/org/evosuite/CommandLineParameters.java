/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.evosuite.classpath.ClassPathHandler;
import org.evosuite.coverage.patch.PatchCoverageTestFitness;
import org.evosuite.coverage.patch.PatchLineCoverageFactory;
import org.evosuite.coverage.patch.communication.json.FixLocation;
import org.evosuite.executionmode.*;
import org.evosuite.utils.ArrayUtil;
import org.evosuite.utils.LoggingUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to define and validate the input parameters passed by console
 *
 * @author arcuri
 */
public class CommandLineParameters {

    /**
     * Validate all the "-" options set on the command line and all
     * the already handled -D ones in Properties
     *
     * @param line
     */
    public static void validateInputOptionsAndParameters(CommandLine line) throws IllegalArgumentException {

        /*
         * TODO: here there is lot more that could be added
         */

        java.util.Properties properties = line.getOptionProperties("D");

        String cut = line.getOptionValue("class");

        if (cut != null) {
            if (cut.endsWith(".java")) {
                throw new IllegalArgumentException("The target -class should be a JVM qualifying name (e.g., org.foo.SomeClass) and not a source file");
            }
            if (cut.endsWith(".class")) {
                throw new IllegalArgumentException("The target -class should be a JVM qualifying name (e.g., org.foo.SomeClass) and not a bytecode file");
            }
        }

        if (!line.hasOption(Continuous.NAME) && !line.hasOption("startedByCtg")) {
            for (Object p : properties.keySet()) {
                if (p.toString().startsWith("ctg_")) {
                    throw new IllegalArgumentException("Option " + p + " is only valid in '-" + Continuous.NAME + "' mode");
                }
            }
        }

        String junitSuffix = properties.getProperty("junit_suffix");
        if (junitSuffix != null && !junitSuffix.endsWith("Test")) {
            throw new IllegalArgumentException("A JUnit suffix should always end with a 'Test'");
        }
    }


    /**
     * Return all the available command line options that can be used with "-"
     *
     * @return
     */
    public static Options getCommandLineOptions() {
        Options options = new Options();

        Option help = Help.getOption();
        Option setup = Setup.getOption();
        Option measureCoverage = MeasureCoverage.getOption();
        Option listClasses = ListClasses.getOption();
        Option listDependencies = WriteDependencies.getOption();
        Option printStats = PrintStats.getOption();
        Option listParameters = ListParameters.getOption();
        Option continuous = Continuous.getOption();

        Option[] generateOptions = TestGeneration.getOptions();

        Option targetClass = new Option("class", true,
                "target class for test generation. A fully qualifying needs to be provided, e.g. org.foo.SomeClass");
        Option targetPrefix = new Option("prefix", true,
                "target package prefix for test generation. All classes on the classpath with the given package prefix " +
                        "will be used, i.e. all classes in the given package and sub-packages.");
        Option targetCP = new Option("target", true,
                "target classpath for test generation. Either a jar file or a folder where to find the .class files");

        Option projectCP = new Option("projectCP", true,
                "classpath of the project under test and all its dependencies");

        Option evosuiteCP = new Option("evosuiteCP", true,
                "classpath of EvoSuite jar file(s). This is needed when EvoSuite is called in plugins like Eclipse/Maven");

        Option junitPrefix = new Option("junit", true, "junit prefix");
        Option criterion = new Option("criterion", true,
                "target criterion for test generation. Can define more than one criterion by using a ':' separated list");
        Option seed = new Option("seed", true, "seed for random number generator");
        Option mem = new Option("mem", true, "heap size for client process (in megabytes)");
        Option libraryPath = new Option("libraryPath", true, "java library path to native libraries of the project under test");
        Option startedByCtg = new Option("startedByCtg", false, "Determine if current process was started by a CTG process");
        Option inheritance = new Option("inheritanceTree", "Cache inheritance tree during setup");
        Option heapDump = new Option("heapdump", "Create heap dump on client VM out of memory error");
        Option base_dir = new Option("base_dir", true, "Working directory in which tests and reports will be placed");

        Option parallel = new Option("parallel", true, "Start parallel run with n clients, communicate every i "
                + "iteration x individuals (rate), expects #num_parallel_clients #migrants_iteration_frequency #migrants_communication_rate");
        parallel.setArgs(3);
        parallel.setArgName("n i x");

        // EvoRepair options
        Option evorepair = new Option("evorepair", true, "EvoRepair execution mode =[testgen|patchgen].");
        Option targetLines = new Option("targetLines", true, "Path to JSON file specifying the initial target lines");
        Option orchestratorPort = new Option("port", true, "Port number of the orchestrator");
        Option seedPopulation = new Option("seeds", true, "Path to JSON file specifying seed population");
        //Option seedKillMatrix = new Option("seedKillMatrix", true, "Path to JSON file of kill matrix w.r.t. previous patch population");
        //Option previousPatchPopulation = new Option("previousPatchPopulation", true, "Path to JSON file of previous patch population");
        //Option updatedPatchPopulation = new Option("nextPatchPopulation", true, "Path to JSON file of updated patch population");
        Option targetPatches = new Option("targetPatches", true, "Path to JSON file specifying target patches");

        @SuppressWarnings("static-access")
        Option property = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator().withDescription("use value for given property").create("D");

        for (Option option : generateOptions) {
            options.addOption(option);
        }

        options.addOption(continuous);
        options.addOption(listParameters);
        options.addOption(help);
        options.addOption(measureCoverage);
        options.addOption(listClasses);
        options.addOption(listDependencies);
        options.addOption(printStats);
        options.addOption(setup);
        options.addOption(targetClass);
        options.addOption(targetPrefix);
        options.addOption(targetCP);
        options.addOption(junitPrefix);
        options.addOption(criterion);
        options.addOption(seed);
        options.addOption(mem);
        options.addOption(libraryPath);
        options.addOption(evosuiteCP);
        options.addOption(inheritance);
        options.addOption(base_dir);
        options.addOption(property);
        options.addOption(projectCP);
        options.addOption(heapDump);
        options.addOption(startedByCtg);
        options.addOption(parallel);
        options.addOption(evorepair);
        options.addOption(orchestratorPort);
        options.addOption(targetLines);
        options.addOption(seedPopulation);
        //options.addOption(seedKillMatrix);
        //options.addOption(previousPatchPopulation);
        //options.addOption(updatedPatchPopulation);
        options.addOption(targetPatches);

        return options;
    }

    public static void handleSeed(List<String> javaOpts, CommandLine line) throws NullPointerException {
        if (line.hasOption("seed")) {
            /*
             * user can both use -seed and -Drandom_seed to set this variable
             */
            String seedValue = line.getOptionValue("seed");
            javaOpts.add("-Drandom_seed=" + seedValue);
            Properties.RANDOM_SEED = Long.parseLong(seedValue);
        }
    }

    /**
     * Add all the properties that were set with -D
     *
     * @param javaOpts
     * @param line
     * @throws Error
     */
    public static void addJavaDOptions(List<String> javaOpts, CommandLine line) throws Error {

        java.util.Properties properties = line.getOptionProperties("D");
        Set<String> propertyNames = new HashSet<>(Properties.getParameters());

        for (String propertyName : properties.stringPropertyNames()) {

            if (!propertyNames.contains(propertyName)) {
                LoggingUtils.getEvoLogger().error("* Unknown property: " + propertyName);
                throw new Error("Unknown property: " + propertyName);
            }

            String propertyValue = properties.getProperty(propertyName);
            javaOpts.add("-D" + propertyName + "=" + propertyValue);
            System.setProperty(propertyName, propertyValue);

            try {
                Properties.getInstance().setValue(propertyName, propertyValue);
            } catch (Exception e) {
                throw new Error("Invalid value for property " + propertyName + ": " + propertyValue + ". Exception " + e.getMessage(), e);
            }
        }
    }

    public static void handleClassPath(CommandLine line) {

        String DCP = null;
        java.util.Properties properties = line.getOptionProperties("D");
        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.equals("CP")) {
                DCP = properties.getProperty(propertyName);
            }
        }

        if (line.hasOption("projectCP") && DCP != null) {
            throw new IllegalArgumentException("Ambiguous classpath: both -projectCP and -DCP are defined");
        }

        String[] cpEntries = null;

        if (line.hasOption("projectCP")) {
            cpEntries = line.getOptionValue("projectCP").split(File.pathSeparator);
        } else if (DCP != null) {
            cpEntries = DCP.split(File.pathSeparator);
        }

        if (cpEntries != null) {
            ClassPathHandler.getInstance().changeTargetClassPath(cpEntries);
        }

        if (line.hasOption("target")) {
            String target = line.getOptionValue("target");

            /*
             * let's just add the target automatically to the classpath.
             * This is useful for when we do not want to specify the classpath,
             * and so just typing '-target' on command line
             *
             */
            ClassPathHandler.getInstance().addElementToTargetProjectClassPath(target);
        }

        if (line.hasOption("evosuiteCP")) {
            String entry = line.getOptionValue("evosuiteCP");
            String[] entries = entry.split(File.pathSeparator);
            ClassPathHandler.getInstance().setEvoSuiteClassPath(entries);
        }
    }

    public static void handleEvoRepairOptions(List<String> javaOpts, CommandLine line) {
        // Enable MOSAPatch
        javaOpts.add("-Dalgorithm=MOSA");
        Properties.getInstance();

        // TODO: Better to set these from cmdline
        Properties.CRITERION = new Properties.Criterion[]{Properties.Criterion.PATCHLINE, Properties.Criterion.PATCH};

        // TODO: Verify if we really need to disable both
        LoggingUtils.getEvoLogger().warn("[EvoRepair] Disabling test minimization and mocking. TODO: Verify if this is really necessary.");
        Properties.MINIMIZE = false;
        Properties.MOCK_IF_NO_GENERATOR = false;

        if (line.hasOption("orchestratorPort")) {
            int port = Integer.parseInt(line.getOptionValue("orchestratorPort"));
            LoggingUtils.getEvoLogger().info("[EvoRepair] Setting orchestrator port to: {}.", port);
            Properties.EVOREPAIR_PORT = port;
        } else {
            LoggingUtils.getEvoLogger().info("[EvoRepair] No orchestrator port specified, defaulting to 7777.");
            Properties.EVOREPAIR_PORT = 7777;
        }

        if (line.hasOption("seeds")) {
            LoggingUtils.getEvoLogger().info("[EvoRepair] Using seeds.");
            Properties.EVOREPAIR_SEED_POPULATION = line.getOptionValue("seeds");
        } else {
            LoggingUtils.getEvoLogger().warn("[EvoRepair] No seeds specified, enable using -seeds option.");
        }

        if (line.hasOption("targetPatches")) {
            Properties.EVOREPAIR_TARGET_PATCHES = line.getOptionValue("targetPatches");
        } else {
            LoggingUtils.getEvoLogger().error("No target patches provided, specify using -targetPatches option.");
            throw new IllegalArgumentException("Missing target patches.");
        }

        /**
        boolean seedPopulation = line.hasOption("seedPopulation");
        boolean previousPatchPopulation = line.hasOption("previousPatchPopulation");
        boolean seedKillMatrix = line.hasOption("seedKillMatrix");
        boolean updatedPatchPopulation = line.hasOption("updatedPatchPopulation");

        // Either all options are used, or none
        if (seedPopulation && previousPatchPopulation && seedKillMatrix && updatedPatchPopulation) {
            LoggingUtils.getEvoLogger().info("[EvoRepair] Using seed information.");
            Properties.EVOREPAIR_SEED_POPULATION = line.getOptionValue("seedPopulation");
            Properties.EVOREPAIR_PREVIOUS_PATCH_POPULATION = line.getOptionValue("previousPatchPopulation");
            Properties.EVOREPAIR_SEED_KILL_MATRIX = line.getOptionValue("seedKillMatrix");
            Properties.EVOREPAIR_UPDATED_PATCH_POPULATION = line.getOptionValue("updatedPatchPopulation");

        } else if (seedPopulation || previousPatchPopulation || seedKillMatrix || updatedPatchPopulation) {
            LoggingUtils.getEvoLogger().error("Unable to use seed information. At least one of the seed options "
                    + "[seedPopulation|previousPatchPopulation|seedKillMatrix|updatedPatchPopulation] is missing.");
        }*/
    }

    public static void handleTargetLines(CommandLine line) {

        if (!line.hasOption("targetLines")) {
            LoggingUtils.getEvoLogger().info("* No target lines specified: specify using -targetLines.");
            return;
        } else {
            if (line.getOptionValue("targetLines") != null) {
                throw new RuntimeException("TODO: Remove this option.");
            }
        }

        String targetLinesPath = line.getOptionValue("targetLines");
        try {
            List<FixLocation> targetLineSpecs = new ObjectMapper().readValue(new File(targetLinesPath), new TypeReference<List<FixLocation>>(){});

            for (FixLocation s : targetLineSpecs) {
                PatchLineCoverageFactory.addTargetLine(s.getClassname(), s.getTargetLines());
            }

        } catch (JsonMappingException e) {
            throw new Error("Error Unable to create instance of TargetLineSpec: " + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new Error("Error while processing target lines JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new Error("Unable to find targetLines file: " + e.getMessage());
        }
    }


        public static void handleJVMOptions(List<String> javaOpts, CommandLine line) {
        /*
         * NOTE: JVM arguments will not be passed over from the master to the client. So for -Xmx, we need to use "mem"
         */
        if (line.hasOption("mem")) {
            javaOpts.add("-Xmx" + line.getOptionValue("mem") + "M");
        }
        if (line.hasOption("libraryPath")) {
            javaOpts.add("-Djava.library.path=" + line.getOptionValue("libraryPath"));
        }

        if (line.hasOption("heapdump")) {
            javaOpts.add("-XX:+HeapDumpOnOutOfMemoryError");
        }
    }
}
