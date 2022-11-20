package edu.coursera.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Class wrapping methods for implementing reciprocal array sum in parallel.
 */
public final class ReciprocalArraySum {

    /**
     * Default constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Sequentially compute the sum of the reciprocal values for a given array.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * Computes the size of each chunk, given the number of chunks to create
     * across a given number of elements.
     *
     * @param nChunks   The number of chunks to create
     * @param nElements The number of elements to chunk across
     * @return The default chunk size
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Integer ceil
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Computes the inclusive element index that the provided chunk starts at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the start of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The inclusive index that this chunk starts at in the set of
     *         nElements
     */
    private static int getChunkStartInclusive(final int chunk,
            final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Computes the exclusive element index that the provided chunk ends at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the end of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The exclusive end index for this chunk
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * This class stub can be filled in to implement the body of each task
     * created to perform reciprocal array sum in parallel.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        static int SEQUENTIAL_THRESHHOLD = 10_000;
        static int numberOfComputeRuns = 0;
        /**
         * Starting index for traversal done by this task.
         */
        private final int startIndexInclusive;
        /**
         * Ending index for traversal done by this task.
         */
        private final int endIndexExclusive;
        /**
         * Input array to reciprocal sum.
         */
        private final double[] input;
        /**
         * Intermediate value produced by this task.
         */
        private double value;

        private int numTasks;

        /**
         * Constructor.
         * 
         * @param setStartIndexInclusive Set the starting index to begin
         *                               parallel traversal at.
         * @param setEndIndexExclusive   Set ending index for parallel traversal.
         * @param setInput               Input values
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive, final int setEndIndexExclusive,
                final double[] setInput, int numTasks) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
            this.numTasks = numTasks;
        }

        /**
         * Getter for the value produced by this task.
         * 
         * @return Value produced by this task
         */
        public double getValue() {
            return value;
        }

        public void oneTaskForkMultipleTimes(boolean debug, boolean useInvokeAll, int nForks) {
            if (debug)
                numberOfComputeRuns++;
            if (endIndexExclusive - startIndexInclusive <= SEQUENTIAL_THRESHHOLD) {
                if (debug)
                    System.out.println("calculating sequentially");

                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    value += 1 / input[i];
                }

            } else {
                ArrayList<ReciprocalArraySumTask> chunks = new ArrayList<>();

                for (int fork = 0; fork < nForks; fork++) {
                    int chunkStartIndex = startIndexInclusive
                            + getChunkStartInclusive(fork, nForks, endIndexExclusive - startIndexInclusive);
                    int chunkEndIndex = startIndexInclusive
                            + getChunkEndExclusive(fork, nForks, endIndexExclusive - startIndexInclusive);

                    ReciprocalArraySumTask chunk = new ReciprocalArraySumTask(
                            chunkStartIndex,
                            chunkEndIndex,
                            input, numTasks);
                    chunks.add(chunk);
                    if (debug)
                        System.out.printf("startIndexChunk:%d | endIndexChunk:%d | numberOfComputeRuns:%d%n",
                                chunkStartIndex,
                                chunkEndIndex,
                                numberOfComputeRuns);
                }
                if (useInvokeAll)
                    ForkJoinTask.invokeAll(chunks);
                else {
                    for (int chunkNumber = 0; chunkNumber < chunks.size() - 1; chunkNumber++) {
                        chunks.get(chunkNumber).fork();
                    }
                    chunks.get(chunks.size() - 1).compute();

                    for (int chunkNumber = 0; chunkNumber < chunks.size() - 1; chunkNumber++) {
                        chunks.get(chunkNumber).join();
                    }
                }

                for (ReciprocalArraySumTask chunk : chunks) {
                    value += chunk.value;
                }

            }
        }

        public void multipleTasksForkTwoTimes(boolean debug, boolean useInvokeAll) {
            if (debug)
                numberOfComputeRuns++;
            if (endIndexExclusive - startIndexInclusive <= SEQUENTIAL_THRESHHOLD) {
                if (debug)
                    System.out.println("calculating sequentially");

                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    value += 1 / input[i];
                }

            } else {
                if (debug)
                    System.out.printf(
                            "startIndex:%d | middleIndex:%d | endIndex:%d | chunkSize:%d | numberOfComputeRuns:%d%n",
                            startIndexInclusive,
                            startIndexInclusive + (endIndexExclusive - startIndexInclusive) / 2,
                            endIndexExclusive, endIndexExclusive - startIndexInclusive,
                            numberOfComputeRuns);

                int middleIndex = startIndexInclusive + (endIndexExclusive -
                        startIndexInclusive) / 2;

                ReciprocalArraySumTask left = new ReciprocalArraySumTask(startIndexInclusive,
                        middleIndex, input, numTasks);
                ReciprocalArraySumTask right = new ReciprocalArraySumTask(middleIndex,
                        endIndexExclusive, input, numTasks);

                if (useInvokeAll)
                    ReciprocalArraySumTask.invokeAll(left, right);
                else {
                    left.fork();
                    right.compute();
                    left.join();
                }

                value = left.value + right.value;
            }
        }

        @Override
        protected void compute() {
            // oneTaskForkMultipleTimes(false, true, this.numTasks); // 1.5x speed up with
            // invokeAll and
            // 1.0x without
            multipleTasksForkTwoTimes(false, true); // 1.5x speedup with or without
            // invokeAll
        }
    }

    /**
     * This class stub can be filled in to implement the body of each task
     * created to perform reciprocal array sum in parallel.
     */
    private static class ReciprocalArraySumTaskRecursive extends RecursiveAction {
        static int SEQUENTIAL_THRESHHOLD = 10_000;
        static int numberOfComputeRuns = 0;
        /**
         * Starting index for traversal done by this task.
         */
        private final int startIndexInclusive;
        /**
         * Ending index for traversal done by this task.
         */
        private final int endIndexExclusive;
        /**
         * Input array to reciprocal sum.
         */
        private final double[] input;
        /**
         * Intermediate value produced by this task.
         */
        private double value;

        private int numTasks;

        /**
         * Constructor.
         * 
         * @param setStartIndexInclusive Set the starting index to begin
         *                               parallel traversal at.
         * @param setEndIndexExclusive   Set ending index for parallel traversal.
         * @param setInput               Input values
         */
        ReciprocalArraySumTaskRecursive(final int setStartIndexInclusive, final int setEndIndexExclusive,
                final double[] setInput, int numTasks) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
            this.numTasks = numTasks;
        }

        /**
         * Getter for the value produced by this task.
         * 
         * @return Value produced by this task
         */
        public double getValue() {
            return value;
        }

        public void oneTaskForkMultipleTimes(boolean debug, boolean useInvokeAll, int nForks) {
            if (debug)
                numberOfComputeRuns++;
            if (endIndexExclusive - startIndexInclusive <= SEQUENTIAL_THRESHHOLD) {
                if (debug)
                    System.out.println("calculating sequentially");

                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    value += 1 / input[i];
                }

            } else {
                ArrayList<ReciprocalArraySumTask> chunks = new ArrayList<>();

                for (int fork = 0; fork < nForks; fork++) {
                    int chunkStartIndex = startIndexInclusive
                            + getChunkStartInclusive(fork, nForks, endIndexExclusive - startIndexInclusive);
                    int chunkEndIndex = startIndexInclusive
                            + getChunkEndExclusive(fork, nForks, endIndexExclusive - startIndexInclusive);

                    ReciprocalArraySumTask chunk = new ReciprocalArraySumTask(
                            chunkStartIndex,
                            chunkEndIndex,
                            input, numTasks);
                    chunks.add(chunk);
                    if (debug)
                        System.out.printf("startIndexChunk:%d | endIndexChunk:%d | numberOfComputeRuns:%d%n",
                                chunkStartIndex,
                                chunkEndIndex,
                                numberOfComputeRuns);
                }
                if (useInvokeAll)
                    ForkJoinTask.invokeAll(chunks);
                else {
                    for (int chunkNumber = 0; chunkNumber < chunks.size() - 1; chunkNumber++) {
                        chunks.get(chunkNumber).fork();
                    }
                    chunks.get(chunks.size() - 1).compute();

                    for (int chunkNumber = 0; chunkNumber < chunks.size() - 1; chunkNumber++) {
                        chunks.get(chunkNumber).join();
                    }
                }

                for (ReciprocalArraySumTask chunk : chunks) {
                    value += chunk.value;
                }

            }
        }

        @Override
        protected void compute() {
            oneTaskForkMultipleTimes(false, true, numTasks);
        }
    }

    /**
     * Modify this method to compute the same reciprocal sum as
     * seqArraySum, but use two tasks running in parallel under the Java Fork
     * Join framework. You may assume that the length of the input array is
     * evenly divisible by 2.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        // return parManyTaskArraySum(input, 2);

        ReciprocalArraySumTask t = new ReciprocalArraySumTask(getChunkStartInclusive(0, 2,
                input.length), getChunkEndExclusive(0, 2, input.length), input, 2);
        ReciprocalArraySumTask t1 = new ReciprocalArraySumTask(getChunkStartInclusive(1, 2,
                input.length), getChunkEndExclusive(1, 2, input.length), input, 2);

        // ForkJoinPool f = ForkJoinPool.commonPool();

        ForkJoinPool f = new ForkJoinPool(2);
        // f.execute(t1);
        f.invoke(t1);
        f.invoke(t);

        return t.value + t1.value;
    }

    /**
     * TODO: Extend the work you did to implement parArraySum to use a set
     * number of tasks to compute the reciprocal array sum. You may find the
     * above utilities getChunkStartInclusive and getChunkEndExclusive helpful
     * in computing the range of element indices that belong to each chunk.
     *
     * @param input    Input array
     * @param numTasks The number of tasks to create
     * @return The sum of the reciprocals of the array input
     */
    protected static double parManyTaskArraySum(final double[] input,
            final int numTasks) {
        // return parManyTaskArraySumNonRecursive(input,numTasks);
        // System.setProperty("java.util.concurrent." + "ForkJoinPool.common." +
        // "parallelism", "4");

        // ReciprocalArraySumTask t = new ReciprocalArraySumTask(0, input.length, input,
        // numTasks);

        ReciprocalArraySumTask t = new ReciprocalArraySumTask(getChunkStartInclusive(0, 4,
                input.length), getChunkEndExclusive(0, 4, input.length), input, numTasks);

        ReciprocalArraySumTask t1 = new ReciprocalArraySumTask(getChunkStartInclusive(1, 4,
                input.length), getChunkEndExclusive(1, 4, input.length), input, numTasks);

        ReciprocalArraySumTask t2 = new ReciprocalArraySumTask(getChunkStartInclusive(2, 4,
                input.length), getChunkEndExclusive(2, 4, input.length), input, numTasks);

        ReciprocalArraySumTask t3 = new ReciprocalArraySumTask(getChunkStartInclusive(3, 4,
                input.length), getChunkEndExclusive(3, 4, input.length), input, numTasks);

        // ReciprocalArraySumTask[] ta = new ReciprocalArraySumTask[2];
        // ta[0] = t;
        // ta[1] = t1;

        // ForkJoinPool f = ForkJoinPool.commonPool();

        ForkJoinPool f = new ForkJoinPool(numTasks);
        f.execute(t1);
        f.execute(t2);
        f.execute(t3); 
        
        // f.invoke(t1);
        // f.invoke(t2);
        // f.invoke(t3);
        f.invoke(t);

        // f.invokeAll(ta);

        // return t.value;
        return t.getValue() + t1.getValue() + t2.getValue() + t3.getValue();
    }

    /**
     * in this implementation the numTasks is the number of tasks submitted to the
     * shared queue
     * 
     * @param input
     * @param numTasks
     */
    public static double parManyTaskArraySumNonRecursive(double[] input, int numTasks) {
        ReciprocalArraySumTask[] array = new ReciprocalArraySumTask[numTasks];

        for (int i = 0; i < numTasks; i++) {
            array[i] = new ReciprocalArraySumTask(getChunkStartInclusive(i, numTasks,
                    input.length), getChunkEndExclusive(i, numTasks, input.length), input, numTasks);
        }

        ForkJoinPool f = new ForkJoinPool(numTasks);

        for (int index = 0; index < array.length; index++) {
            if (index == (array.length - 1)) {
                f.invoke(array[index]);
            } else {
                f.execute(array[index]);
            }
        }

        double value = 0;
        for (ReciprocalArraySumTask reciprocalArraySumTask : array) {
            value += reciprocalArraySumTask.value;
        }
        return value;
    }

    /**
     * in this implementation the numTasks is the number of sub-tasks in each layer
     * of recursion
     * 
     * @param input
     * @param numTasks
     */
    public double parManyTaskArraySumRecursive(double[] input, int numTasks) {
        ReciprocalArraySumTaskRecursive t = new ReciprocalArraySumTaskRecursive(0, input.length, input,
                numTasks);

        ForkJoinPool f = new ForkJoinPool(numTasks);
        f.invoke(t);

        return t.value;

    }

}
