java -jar master/target/evosuite-master-1.2.0.jar -class org.apache.commons.math.optimization.fitting.GaussianFitter -evorepair testgen -targetPatches /home/lam/workspace/nus-apr/evoRepair/output/math_58-math_58-230119_174539/gen-test/target_patches_gen1.json -oracleLocations /home/lam/workspace/nus-apr/defects4j-instrumented/oracle-locations/math_58.json -projectCP /home/lam/workspace/nus-apr/defects4j-instrumented/instrumented-archives/math_58/target/classes -generateMOSuite -Dclient_on_thread=false
