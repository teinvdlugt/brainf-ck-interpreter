package com.teinvdlugt.android.brainfuckinterpreter;

import android.app.Activity;

public class Interpreter {

    private CellsAdapter adapter;
    private Activity context;
    private Listener listener;

    private int delay = 0;
    private boolean running = false;
    private int ptr = 0; // TODO eliminate variable?
    private int i = 0;
    private byte input = -1;

    public interface Listener {
        void doOutput(byte output);
        void askForInput();
        void onFinished();
        void onError();
        void onErrorMaximumCells();
    }

    public Interpreter(Activity context, Listener listener, CellsAdapter adapter, int delay) {
        this.adapter = adapter;
        this.context = context;
        this.listener = listener;
        this.delay = delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void run(String code) {
        adapter.clearMemory();
        adapter.movePointer(0);
        ptr = 0;
        i = 0;
        input = -1;

        running = true;

        interpret(code);
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }


    private String code;

    private void interpret(final String code) {
        this.code = code;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean callOnFinished = true; // Set to false if while-loop terminates because of input

                while (i < code.length() && running) {
                    char token = code.charAt(i);
                    boolean noDelay = false; // Set to false if current character is a comment character
                    try {
                        if (token == '>') {
                            ptr++;
                            i++;

                            if (ptr >= CellsLayout.MAX_CELL_AMOUNT) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onErrorMaximumCells();
                                    }
                                });
                                break;
                            }

                            adapter.movePointer(ptr);
                        } else if (token == '<') {
                            ptr--;
                            i++;

                            if (ptr < 0) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onError();
                                    }
                                });
                                break;
                            }

                            adapter.movePointer(ptr);
                        } else if (token == '+') {
                            adapter.incrementPointedCellValue();
                            i++;
                        } else if (token == '-') {
                            adapter.decrementPointedCellValue();
                            i++;
                        } else if (token == ',') {
                            if (input == -1 || input == 255 /* Weird bytes sometimes say that they're 255 */) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.askForInput();
                                    }
                                });
                                callOnFinished = false;
                                break;
                            } else {
                                adapter.setPointedCellValue(input);
                                i++;
                                input = -1;
                                callOnFinished = true;
                            }
                        } else if (token == '.') {
                            final byte output = adapter.getPointedCellValue();
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.doOutput(output);
                                }
                            });
                            i++;
                        } else if (token == '[') {
                            if (adapter.getPointedCellValue() == 0) {
                                i = matchingClosingBracket(code, i) + 1;
                            } else {
                                i++;
                            }
                        } else if (token == ']') {
                            if (adapter.getPointedCellValue() == 0) {
                                i++;
                            } else {
                                i = matchingOpeningBracket(code, i) + 1;
                            }
                        } else {
                            // Invalid character: skip
                            i++;
                            noDelay = true;
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        // Exception can be thrown when invoking matchingClosingBracket()
                        // while there is none
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError();
                            }
                        });
                        break;
                    }

                    if (!noDelay) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (running && callOnFinished) {
                    running = false;
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFinished();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Called by the activity when input is provided by the user
     */
    public void continueOnInput(byte input) {
        this.input = input;
        interpret(code);
    }

    /**
     * Returns position of ] bracket in code matching the [ bracket at position i
     */
    private static int matchingClosingBracket(String code, int i) {
        int openingBrackets = 0;
        while (true) {
            i++;
            if (code.charAt(i) == '[') {
                openingBrackets++;
            } else if (code.charAt(i) == ']') {
                if (openingBrackets == 0) return i;
                openingBrackets--;
            }
        }
    }

    /**
     * Returns position of [ bracket in code matching the ] bracket at position i
     */
    private static int matchingOpeningBracket(String code, int i) {
        int closingBrackets = 0;
        while (true) {
            i--;
            if (code.charAt(i) == ']') {
                closingBrackets++;
            } else if (code.charAt(i) == '[') {
                if (closingBrackets == 0) return i;
                closingBrackets--;
            }
        }
    }
}
