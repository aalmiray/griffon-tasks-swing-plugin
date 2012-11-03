/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.tasks.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;

/**
 * @author Andres Almiray
 */
public class BlockingGlassPane extends JComponent {
    private static final KeyListener EMPTY_KEY_LISTENER = new KeyAdapter() {
    };
    private static final MouseListener EMPTY_MOUSE_LISTENER = new MouseAdapter() {
    };

    private Color color1;
    private Color color2;

    public BlockingGlassPane() {
        setOpaque(true);
        this.color1 = new Color(0, 0, 0, 175);
        this.color2 = new Color(0, 0, 0, 120);
        addKeyListener(EMPTY_KEY_LISTENER);
        addMouseListener(EMPTY_MOUSE_LISTENER);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint fill = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
        g2d.setPaint(fill);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        setOpaque(false);
        super.paintComponent(g);
        setOpaque(true);
    }
}
