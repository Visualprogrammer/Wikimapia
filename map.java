import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Math.PI;
import static java.lang.Math.random;

public class map extends JPanel implements KeyEventDispatcher, Runnable, MouseMotionListener {
    Boolean needDownload = true;
    int lengtharrayoftile = 0;
    int leftTileX;
    int leftTileY;
    // int leftx; возможно не нужно
    // int lefty;
    int zoom; // приближение
    double lat = 0; // широта
    double lon = 0; // долгота
    int maxTileByX = 0; // максимальное количество квадратов по ширине окна
    int maxTileByY = 0; // максимальное количество квадратов по высоте окна
    int h; // высота окна
    int w; // ширина окна
    int x; // координата x карты в проекции Меркатора
    int y; // координата y карты в проекции Меркатора
    int shiftX; // сдвиг по x квадрата карты
    int shiftY; // сдвиг по y квадрата карты
    int usershiftXDown = 10; // = 10 сдвиг по нажатию кнопки
    int usershiftYRight = 10; // = 10 сдвиг по нажатию кнопки
    int usershiftXUp = -10; // = - 10 сдвиг по нажатию кнопки
    int usershiftYLeft = -10; // = - 10 сдвиг по нажатию кнопки
    // Boolean[] tileImageStatus;// двумерный массив статуса загрузки [max x][max y]
    //Integer[] xTilesNum; // номер по x квадрата
    // Integer[] yTilesNum; // номер  по y квадрата
    // BufferedImage[] tileImage; // одномерный массив изображений [max x][max y]
    String server;
    tiles[] tiles = new tiles[22 - 2]; // от 3 до 22
    @Override
    public void run() { //второй поток (загрузка изображений)
        if (needDownload) {
            needDownload = false;
            int centerX = (int) Math.floor(x / 256);
            int centerY = (int) Math.floor(y / 256);
            viewDiagn(centerX + " x центр " + centerY + " y центр");
            int startX = (int) (centerX - 5);// Math.floor(maxTileByX / 2));
            int startY = (int) (centerY - 5);//Math.floor(maxTileByY / 2));
            leftTileX = startX;
            leftTileY = startY;
            //  viewDiagn("Download started");
            int a = 0;
            for (int i = 0; i < maxTileByX; i++) {
                for (int p = 0; p < maxTileByY; p++) {
                    int tilex = startX + i;
                    int tiley = startY + p;
                    try {
                        tiles[zoom - 3].DownloadFile(tilex, tiley, server);
                    } catch (IOException e) {

                    }
                }
            }
        }
        //while (false) {
        //  if (needDownload) {
        //    needDownload = false;
        // }
        // try {
        //    Thread.sleep(40);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        // }
        //}
    }

    public map(double latA, double lonA, int z, int hei, int wei, String serv) throws InterruptedException { //
        server = serv;
        zoom = z;
        lat = latA;
        lon = lonA;
        h = hei;
        w = wei;
        viewDiagn("https://tile.openstreetmap.org/" + getTileNumber(lat, lon, zoom) + ".png");
        viewDiagn(getX());
        viewDiagn(getY());
        x = ((int)(getX()/256)) * 256;
        y = ((int)(getY()/256)) * 256;
        shiftX = x - (getTileX() * 256);
        shiftY = y - (getTileY() * 256);
        // leftx = x - (hei/2); вынесено в отрисовку
        //   lefty = y - (wei/2);
        // maxTileByX = (int)(Math.ceil((w/256) + 3));
        // maxTileByY = (int)(Math.ceil((h/256) + 3));
        maxTileByX = 16;
        maxTileByY = 13;
        //   for(int i = 0; i<maxTileByY-1; i++) { // запись статуса не загружено
        //      for(int p = 0; p<maxTileByX; p++) {
        //         tileImageStatus[i][p] = false;
        //     }
        //  }
        for(int i = 0; i < 22 - 2; i++) {
            tiles[i] = new tiles(i + 3);
        }
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(this);
        Thread childTread = new Thread(this);
        childTread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        this.viewDiagn("Repaint");
        this.drawMap(g);
    }

    public String getTileNumber(final double lat, final double lon, final int zoom) { // получение номера квадрата
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        viewDiagn(xtile * 256);
        viewDiagn(ytile * 256);
        return ("" + zoom + "/" + xtile + "/" + ytile);
    }

    public int getX() { // получение координаты x
        double l = lon * PI / 180; // преобразуем в радианы
        int x = (int) ((256 / (2 * PI)) * (Math.pow(2, zoom)) * (l + PI));
        return x;
    }

    public int getY() { // получение координаты y
        double l = lat * PI / 180; // преобразуем в радианы
        int y = (int) ((256 / (2 * PI)) * (Math.pow(2, zoom)) * (PI - Math.log(Math.tan((PI / 4) + (l / 2)))));
        return y;
    }

    public int getTileX() {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return xtile;
    }

    public int getTileY() {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return ytile;
    }

    public void viewDiagn(String a) {
        System.out.println(a);
    }

    public void viewDiagn(int a) {
        System.out.println(a);
    }

    public void viewDiagn(double a) {
        System.out.println(a);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        //    System.out.println("Новое событие кнопок клавиатуры!" + e.getKeyCode());
        String typeOfEvent = "unknown";
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            typeOfEvent = "pressed";
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            typeOfEvent = "released";
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
            typeOfEvent = "typed";
        }
        int centerx = (x - shiftX) / 256;
        int centery = (y - shiftY) / 256;
        String key = "unknown";
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            key = "space";
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            key = "enter";
            shiftX += usershiftYLeft;
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            key = "enter";
            shiftY += usershiftXUp;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            key = "enter";
            shiftX += usershiftYRight;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            key = "enter";
            shiftY += usershiftXDown;
        } else if (e.getKeyCode() == 107) { //приближение плюс
            lengtharrayoftile = 0;
            // tileImage = new BufferedImage[maxTileByY * maxTileByX + 10000]; // создание массива для всех квадратов
            //   tileImageStatus = new Boolean[maxTileByY * maxTileByX + 10000];
            //  xTilesNum = new Integer[maxTileByX * maxTileByY + 10000];
            //  yTilesNum = new Integer[maxTileByY * maxTileByX + 10000];
            tiles[zoom - 3].ClearMemory(4);
            zoom++;
            x = x * 2;
            y = y* 2;
            shiftX *= 2;
            shiftY *= 2;

        }
        else if (e.getKeyCode() == KeyEvent.VK_MINUS) {
            key = "code#" + e.getKeyCode();
            lengtharrayoftile = 0;
            //  tileImage = new BufferedImage[maxTileByY * maxTileByX + 10000]; // создание массива для всех квадратов
            //  tileImageStatus = new Boolean[maxTileByY * maxTileByX + 10000];
            // xTilesNum = new Integer[maxTileByX * maxTileByY + 10000];
            //  yTilesNum = new Integer[maxTileByY * maxTileByX + 10000];
            tiles[zoom - 3].ClearMemory(4);
            zoom--;
            x = x / 2;
            y = y / 2;
            shiftX /= 2;
            shiftY /= 2;
        }

        if (shiftX > 256) {
            x = x + 256;
            shiftX = shiftX - 256;
            //   needDownload = true;
        }
        if (shiftY > 256) {

            y = y + 256;
            shiftY = shiftY - 256;
            //    needDownload = true;
        }
        if (shiftX < -256) {

            x -= 256;
            shiftX += 256;
            //   needDownload = true;

        }
        if (shiftY < -256) {

            y -= 256;
            shiftY += 256;
            //   needDownload = true;

        }
        for (int r = 0; r < tiles.length; r++){
            if(!(r == zoom - 3)) {
                tiles[r].ClearMemory(0);
            } else {
                tiles[r].ClearMemory(1);
            }
        }
        viewDiagn((String.valueOf(x/256 + " " + y/256)));
        //    System.out.println("type=" + typeOfEvent + " keyCode=" + key);
        return false;
    }

    // private String DownloadFile(String url, int tilex, int tiley) throws IOException {
    //    URL Url = new URL(url);
    //  BufferedImage img = ImageIO.read(Url.openStream());
    //   if (!Files.exists(Paths.get("D:\\image\\" + zoom + "\\" + tilex + "\\"))) {
    //       Files.createDirectories(Paths.get("D:\\image\\" + zoom + "\\" + tilex + "\\"));
    //   }
    //   if (new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png").exists()) {
    ///       //  new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png").delete();
    //  } else {
    //       File file = new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png");
    ///      ImageIO.write(img, "png", file);
    //  }
    //  return null;
    // }

    // private BufferedImage DownFile(String url, int tilex, int tiley) throws IOException {
    ///     URL Url = new URL(url);
    //    BufferedImage img = ImageIO.read(Url.openStream());
    //     if (!Files.exists(Paths.get("D:\\image\\" + zoom + "\\" + tilex + "\\"))) {
    //       Files.createDirectories(Paths.get("D:\\image\\" + zoom + "\\" + tilex + "\\"));
    //   }
    //    if (new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png").exists()) {
    //        new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png").delete();
    //    } else {
    //    File file = new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png");
    //      ImageIO.write(img, "png", file);
    //  }
    //   return ImageIO.read(new File("D:\\image\\" + zoom + "\\" + tilex + "\\" + tiley + ".png"));
    //  }

    public void drawMap(Graphics g) {
        // viewDiagn("Отрисовка");
        int zx = x / 256;
        int zy = y/256;
        for(int x1 = (zx - 4); x1 < (zx + 13); x1++) {
            for(int y1 = (zy - 4); y1 < (zy + 13); y1++) {
                try {
                    //    g.fillRect(-x + x1*256 - shiftX, -y + y1*256 - shiftY, 256,256);
                    g.drawImage(tiles[zoom - 3].getImage(x1,y1), -x + x1*256 - shiftX, -y + y1*256 - shiftY, null);
                    //      g.drawString(x1 + " "+ y1,-x + x1*256 - shiftX + 20, -y + y1*256 - shiftY +120);
                }
                catch(Exception e) {}
            }
        }
        // for (int i1 = 0; i1 < lengtharrayoftile; i1++) {
        //   // g.fillRect(p*256 - shiftX, i*256 - shiftY, 256,256);
        // try {
        //   // if (tileImageStatus[i1][p1]) {
        //   if ((Math.abs(xTilesNum[i1] - (x/256)) < 20) && ((Math.abs(yTilesNum[i1] - (y/256))) < 20)) {
        //      g.drawImage(tileImage[i1], xTilesNum[i1]*256 - x - shiftX + 512, yTilesNum[i1] * 256 - y - shiftY+512, null);
        //   }
        //} catch (Exception ee) {

        // }
        // }
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
    // public Boolean getNeedDownload(int x, int y) {
    //      Boolean c = true;
    //     for(int a = 0; a<lengtharrayoftile + 1; a++) {
    //       if(x == xTilesNum[a] && y == yTilesNum[a]) {
    ///            c = false;
    //        } else {
    //        }
    //    }
    //     return c;
    //}

}
