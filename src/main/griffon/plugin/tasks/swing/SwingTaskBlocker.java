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

import griffon.plugins.tasks.Task;
import org.codehaus.griffon.runtime.tasks.AbstractTaskBlocker;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andres Almiray
 */
public class SwingTaskBlocker extends AbstractTaskBlocker {
    private static final WindowStore windowStore = new WindowStore();
    private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);

    public void block(Task task) {
        switch (task.getMode()) {
            case BLOCKING_APPLICATION:
                for (Window window : Window.getWindows()) {
                    block(task, window);
                }
                break;
            case BLOCKING_WINDOW:
                block(task, findFocusedWindow());
                break;
            default:
                // ignore
        }
    }

    public void unblock(Task task) {
        switch (task.getMode()) {
            case BLOCKING_APPLICATION:
                for (Window window : Window.getWindows()) {
                    unblock(task, window);
                }
                break;
            case BLOCKING_WINDOW:
                unblock(task, null);
                break;
            default:
                // ignore
        }
    }

    private void block(Task task, Window window) {
        if (window instanceof RootPaneContainer && !windowStore.contains(window, task)) {
            boolean alreadyBlocked = windowStore.contains(window);
            windowStore.add(window, task);
            Component glassPane = ((RootPaneContainer) window).getGlassPane();
            if (!alreadyBlocked) {
                window.setCursor(WAIT_CURSOR);
                if (!(glassPane instanceof BlockingGlassPane)) {
                    glassPane = new BlockingGlassPane();
                    ((RootPaneContainer) window).setGlassPane(glassPane);
                }
            }
            glassPane.setVisible(true);
        }
    }

    private void unblock(Task task, Window window) {
        WindowState windowState = null;
        if (window == null) {
            windowState = windowStore.findWindowStateFor(task);
            if (windowState != null) {
                windowStore.remove(windowState.window, task);
            }
        } else if (window instanceof RootPaneContainer && windowStore.contains(window, task)) {
            windowState = windowStore.remove(window, task);
        }

        if (windowState != null && !windowStore.contains(windowState)) {
            windowState.window.setCursor(windowState.cursor);
            if (windowState.glassPane != null) {
                ((RootPaneContainer) windowState.window).setGlassPane(windowState.glassPane);
                windowState.glassPane.setVisible(false);
            }
        }
    }

    private Window findFocusedWindow() {
        for (Window window : Window.getWindows()) {
            if (window.isFocused()) {
                return window;
            }
        }
        return null;
    }

    private static class WindowState {
        private final Window window;
        private final Cursor cursor;
        private Component glassPane;

        private static WindowState create(Window window) {
            Component glassPane = window instanceof RootPaneContainer ? ((RootPaneContainer) window).getGlassPane() : null;
            return new WindowState(window, window.getCursor(), glassPane);
        }

        private WindowState(Window window, Cursor cursor, Component glassPane) {
            this.window = window;
            this.cursor = cursor;
            this.glassPane = glassPane;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WindowState that = (WindowState) o;

            if (window != null ? !window.equals(that.window) : that.window != null) return false;
            if (cursor != null ? !cursor.equals(that.cursor) : that.cursor != null) return false;
            if (glassPane != null ? !glassPane.equals(that.glassPane) : that.glassPane != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = window != null ? window.hashCode() : 0;
            result = 31 * result + (cursor != null ? cursor.hashCode() : 0);
            result = 31 * result + (glassPane != null ? glassPane.hashCode() : 0);
            return result;
        }
    }

    private static final class WindowStore {
        private final Map<WindowState, List<Task<?, ?>>> cache = new ConcurrentHashMap<WindowState, java.util.List<Task<?, ?>>>();

        private boolean contains(Window window) {
            return findWindowStateFor(window) != null;
        }

        private boolean contains(WindowState windowState) {
            return windowState != null && findWindowStateFor(windowState.window) != null;
        }

        private boolean contains(Window window, Task task) {
            List<Task<?, ?>> tasks = findTasksFor(window);
            return tasks != null && tasks.contains(task);
        }

        private WindowState add(Window window, Task<?, ?> task) {
            WindowState windowState = findWindowStateFor(window);
            if (windowState == null) windowState = WindowState.create(window);
            List<Task<?, ?>> tasks = cache.get(windowState);
            if (tasks == null) {
                tasks = new ArrayList<Task<?, ?>>();
                cache.put(windowState, tasks);
            }
            if (!tasks.contains(task)) tasks.add(task);
            return windowState;
        }

        private WindowState remove(Window window, Task task) {
            WindowState windowState = findWindowStateFor(window);
            List<Task<?, ?>> tasks = windowState != null ? cache.get(windowState) : null;
            if (tasks != null) {
                tasks.remove(task);
                if (tasks.isEmpty()) {
                    cache.remove(windowState);
                }
            }
            return windowState;
        }

        private List<Task<?, ?>> findTasksFor(Window window) {
            synchronized (cache) {
                for (Map.Entry<WindowState, List<Task<?, ?>>> entry : cache.entrySet()) {
                    if (entry.getKey().window == window) {
                        return entry.getValue();
                    }
                }
            }
            return null;
        }

        private WindowState findWindowStateFor(Window window) {
            synchronized (cache) {
                for (WindowState windowState : cache.keySet()) {
                    if (windowState.window == window) {
                        return windowState;
                    }
                }
            }
            return null;
        }

        private WindowState findWindowStateFor(Task task) {
            synchronized (cache) {
                for (Map.Entry<WindowState, List<Task<?, ?>>> entry : cache.entrySet()) {
                    List<Task<?, ?>> tasks = entry.getValue();
                    if (tasks != null && tasks.contains(task)) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }
    }
}
