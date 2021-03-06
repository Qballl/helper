/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.helper.terminable;

import me.lucko.helper.terminable.registry.TerminableRegistry;

import javax.annotation.Nonnull;

public final class Terminables {

    @Nonnull
    public static Terminable combine(@Nonnull Terminable... terminables) {
        if (terminables.length == 0) {
            return Terminable.EMPTY;
        }
        if (terminables.length == 1) {
            return terminables[0];
        }

        TerminableRegistry registry = TerminableRegistry.create();
        for (Terminable terminable : terminables) {
            terminable.bindWith(registry);
        }
        return registry;
    }

    @Nonnull
    public static Terminable combine(@Nonnull Iterable<Terminable> terminables) {
        TerminableRegistry registry = TerminableRegistry.create();
        for (Terminable terminable : terminables) {
            terminable.bindWith(registry);
        }
        return registry;
    }

    public static void silentlyTerminate(Terminable... terminables) {
        for (Terminable t : terminables) {
            if (t == null) {
                continue;
            }

            try {
                t.terminate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void silentlyTerminate(Iterable<? extends Terminable> terminables) {
        for (Terminable t : terminables) {
            if (t == null) {
                continue;
            }

            try {
                t.terminate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Terminables() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
