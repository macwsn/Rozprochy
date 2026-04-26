module Demo {
    exception NotFound { string what; };

    struct State {
        string label;
        long counter;
        double value;
    };

    interface Counter {
        State read();
        void inc(long delta);
        void setLabel(string s);
    };
};
