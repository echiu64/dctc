package com.dataiku.dctc.command.cat;

class SimpleCatPrinter extends AbstractCatPrinter {
    public void print(String line) {
        getHeader().print(line);
        System.out.print(line);
        getEol().print();
    }
    public void end() {
    }
}
