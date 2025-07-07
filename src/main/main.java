package main;

import util.DBInitializer;
import ui.LoginFrame;

public class main {
    public static void main(String[] args) {
        DBInitializer.createTables(); //önce tablolar eklendi
        DBInitializer.insertSampleUsers(); //sonra giriş ekranı açıldı
        new LoginFrame();
    }
}
