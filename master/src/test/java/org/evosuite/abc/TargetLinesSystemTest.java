package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.patch.PatchCoverageFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class TargetLinesSystemTest extends SystemTestBase {
    @Test
    public void testLoadJSON() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("testTargetLines.json");
        String[] command = new String[] {"-targetLines", resource.getPath(), "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);

        Assert.assertArrayEquals(PatchCoverageFactory.getTargetLinesForClass("some.package.name.Class1"), new int[] {1,2,3});
        Assert.assertArrayEquals(PatchCoverageFactory.getTargetLinesForClass("some.package.name.Class2"), new int[] {4,5,6,7,8,9});
    }
}
