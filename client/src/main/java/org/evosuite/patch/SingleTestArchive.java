package org.evosuite.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SingleTestArchive {

    private static final Logger logger = LoggerFactory.getLogger(SingleTestArchive.class);
    /**
     * Map used to store all covered targets (keys of the map) and the corresponding covering
     * solutions (values of the map)
     */
    private final Map<SingleTestPatchFitness<?>, Set<AbstractPatchChromosome<?>>> covered = new LinkedHashMap<>();
    private final Set<SingleTestPatchFitness<?>> uncovered = new LinkedHashSet<>();

    private SingleTestArchive() {}

    private static final SingleTestArchive instance = new SingleTestArchive();

    public static SingleTestArchive getInstance() {
        return instance;
    }

    public void addTarget(SingleTestPatchFitness<?> target) {
        if (!uncovered.contains(target)) {
            logger.debug("Registering new target '{}'", target);
            uncovered.add(target);
        }
    }

    public void updateArchive(SingleTestPatchFitness<?> target, AbstractPatchChromosome<?> solution) {
        assert this.covered.containsKey(target) || this.uncovered.contains(target) : "Unknown goal: " + target;

        uncovered.remove(target);

        if (!covered.containsKey(target)) {
            covered.put(target, new LinkedHashSet<>());
            logger.debug("Covered target '{}'", target);
        }
        covered.get(target).add(solution);
    }

    public int getNumberOfTargets() {
        return this.covered.keySet().size() + this.uncovered.size();
    }

    public int getNumberOfCoveredTargets() {
        return this.covered.size();
    }

    public Set<SingleTestPatchFitness<?>> getCoveredTargets() {
        return this.covered.keySet();
    }

    public int getNumberOfUncoveredTargets() {
        return this.uncovered.size();
    }

    public Set<SingleTestPatchFitness<?>> getUncoveredTargets() {
        return this.uncovered;
    }

    private Set<SingleTestPatchFitness<?>> getTargets() {
        Set<SingleTestPatchFitness<?>> targets = new LinkedHashSet<>();
        targets.addAll(this.getCoveredTargets());
        targets.addAll(this.getUncoveredTargets());
        return targets;
    }

    public List<AbstractPatchChromosome<?>> getSolutions() {
        List<AbstractPatchChromosome<?>> solutions = new ArrayList<>();
        for (Set<AbstractPatchChromosome<?>> s: this.covered.values()) {
            solutions.addAll(s);
        }
        return solutions;
    }

    public Set<SingleTestPatchFitness<?>> getLeastCoveredTargets() {
        if (!uncovered.isEmpty()) {
            return uncovered;
        }

        int minCount = Integer.MAX_VALUE;
        Map<Integer, Set<SingleTestPatchFitness<?>>> counter = new LinkedHashMap<>();
        for (Map.Entry<SingleTestPatchFitness<?>, Set<AbstractPatchChromosome<?>>> entry: covered.entrySet()) {
            int count = entry.getValue().size();
            if (!counter.containsKey(count)) {
                counter.put(count, new LinkedHashSet<>());
            }
            counter.get(count).add(entry.getKey());

            if (count < minCount) {
                minCount = count;
            }
        }

        return counter.get(minCount);
    }
}