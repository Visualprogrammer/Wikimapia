public class main {
    public static void main(String[] args) throws InterruptedException {
        frame f = new frame(true,100, 100, 800, 500);
        map Map = new map(60, 30, 16, 800, 500, "wikimapia");
        f.add(Map);
        f.setVisible(true);
        while(true) {
            // Map.viewDiagn("Рис.");
            Map.drawMap(f.getGraphics());
            Thread.sleep(40);
        }
    }
}
