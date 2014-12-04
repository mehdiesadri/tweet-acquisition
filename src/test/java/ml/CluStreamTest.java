//package ml;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.junit.Test;
//
//public class CluStreamTest {
//
//    @Test
//    public void test() {
//        List<String[]> d = new ArrayList<String[]>();
//
//        d.add(new String[] { "1", "t1 t2 t3 t4 t5", "1" });
//        d.add(new String[] { "2", "t1 t2 t3", ".8" });
//        d.add(new String[] { "3", "r1 r2 t3 r3", ".95" });
//        d.add(new String[] { "4", "r2 t3 r3 r4 r5", ".85" });
//
//        CluStream clustream = new CluStream();
//        clustream.cluster(d, 2);
//        clustream.cluster(new String[] { "5", "r2 t3 r3 t6", "1" });
//        clustream.cluster(new String[] { "6", "t1 t4 t5", ".95" });
//
//        List<Cluster> clusters = clustream.getClusters();
//        assertEquals(2, clusters.size());
//        assertEquals(3, clusters.get(0).getNumDocs());
//        assertEquals(3, clusters.get(1).getNumDocs());
//
//        List<String> actuals = clusters.get(0).getDocIds();
//        assert Arrays.asList("1", "2", "6").equals(actuals) || Arrays.asList("3", "4", "5").equals(actuals);
//    }
//
//}
