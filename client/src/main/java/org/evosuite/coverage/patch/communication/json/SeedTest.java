package org.evosuite.coverage.patch.communication.json;

import java.util.Set;

public class SeedTest {
    private String name;
    private Set<String> kills;

    public SeedTest(){}
    public SeedTest(String name, Set<String> kills) {
        this.name = name;
        this.kills = kills;
    }

    public String getName() {
        return this.name;
    }

    public void setName() {
        this.name = name;
    }

    public Set<String> getKills() {
        return this.kills;
    }

    public void setKills(Set<String> kills) {
        this.kills = kills;
    }


}
