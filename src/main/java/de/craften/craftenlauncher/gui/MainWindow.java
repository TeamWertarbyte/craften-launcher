/**
 * CraftenLauncher is an alternative Launcher for Minecraft developed by Mojang.
 * Copyright (C) 2014  Johannes "redbeard" Busch, Sascha "saschb2b" Becker
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author saschb2b
 */

package de.craften.craftenlauncher.gui;

import de.craften.craftenlauncher.gui.panel.Header;
import de.craften.craftenlauncher.gui.panel.Loading;
import de.craften.craftenlauncher.gui.panel.Login;
import de.craften.craftenlauncher.gui.panel.Profile;
import de.craften.craftenlauncher.logic.Facade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private Header header;
    private final JPanel body;
    private CardLayout bodyLayout;
    private Login login;
    private Profile profile;
    private Loading loading;
    private Point mousePointer;

    public MainWindow() {
        try {
            setIconImage(new ImageIcon(getClass().getResource("/images/icon.png")).getImage());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        setSize(375, 445);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        centerWindow(this);
        setUndecorated(true);
        //setShape(new RoundRectangle2D.Double(0, 0, _width, _height, 3, 3));
        setVisible(true);
        setLayout(new BorderLayout());

        bodyLayout = new CardLayout(0, 0);
        body = new JPanel();
        body.setLayout(bodyLayout);

        header = new Header();
        add(header, BorderLayout.PAGE_START);
        add(body, BorderLayout.CENTER);

        setTitle("Craften Launcher");
        setResizable(false);

        addLayers();
        addListeners();
    }

    public void reset() {
        body.removeAll();
        addLayers();
    }

    private void addLayers() {
        login = new Login();
        body.add(login, "login");

        profile = new Profile();
        body.add(profile, "profile");

        loading = new Loading();
        body.add(loading, "loading");
        Facade.getInstance().setMinecraftDownloadObserver(loading);

        bodyLayout.show(body, "login");
    }

    private void addListeners(){
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - mousePointer.x, e.getYOnScreen() - mousePointer.y);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });

        addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mousePointer = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    public void showProfile() {
        profile.init();
        bodyLayout.show(body, "profile");

        try {
            setIconImage(new ImageIcon(getClass().getResource("/images/icon2.png")).getImage());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void showLoadingScreen() {
        loading.init();
        bodyLayout.show(body, "loading");
    }

    private void centerWindow(MainWindow frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((screenSize.getWidth() - getWidth()) / 2);
        int y = (int) ((screenSize.getHeight() - getHeight()) / 2);
        frame.setLocation(x, y);
    }

    public void init() {
        login.init();
    }
}
