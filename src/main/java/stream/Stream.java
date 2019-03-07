package stream;

import function.Accumulate;
import function.ForEach;
import function.Function;
import function.Predicate;

/**
 * @Author xiongyx
 * @Date 2019/3/6
 */
public class Stream <T> implements StreamInterface<T> {

    //===============================属性================================

    private T head;

    private boolean isEnd;

    private Process process;

    //==============================构造方法===============================

    public static class Builder<T>{
        private Stream<T> target;

        public Builder() {
            this.target = new Stream<>();
        }

        public Builder<T> head(T head){
            target.head = head;
            return this;
        }

        public Builder<T> isEnd(boolean isEnd){
            target.isEnd = isEnd;
            return this;
        }

        public Builder<T> process(Process process){
            target.process = process;
            return this;
        }

        public Stream<T> build(){
            return target;
        }
    }

    //=================================API实现==============================

    @Override
    public <R> Stream<R> map(Function<R, T> mapper) {
        Process lastProcess = this.process;
        this.process = new Process(
                ()->{
                    Stream stream = lastProcess.eval();
                    return map(mapper,stream);
                }
        );

        // 求值链条 加入一个新的process map
        return new Stream.Builder<R>()
                .process(this.process)
                .build();
    }

    @Override
    public Stream<T> filter(Predicate<T> predicate) {
        Process lastProcess = this.process;
        this.process = new Process(
                ()-> {
                    Stream stream = lastProcess.eval();
                    return filter(predicate,stream);
                }
        );

        // 求值链条 加入一个新的process filter
        return this;
    }

    @Override
    public Stream<T> limit(int n) {
        Process lastProcess = this.process;
        this.process = new Process(
                ()-> {
                    Stream stream = lastProcess.eval();
                    return limit(n,stream);
                }
        );

        // 求值链条 加入一个新的process limit
        return this;
    }

    @Override
    public void forEach(ForEach<T> consumer) {
        // 终结操作 直接开始求值
        forEach(consumer,this.eval());
    }

    @Override
    public <R> R reduce(R initVal, Accumulate<R, T> accumulator) {
        // 终结操作 直接开始求值
        return reduce(initVal,accumulator,this.eval());
    }

    @Override
    public <R, A> R collect(Collector<T, A, R> collector) {
        // 终结操作 直接开始求值
        A result = collect(collector,this.eval());

        // 通过finish方法进行收尾
        return collector.finisher().apply(result);
    }

    //===============================私有方法====================================

    private <R> Stream<R> map(Function<R, T> mapper,Stream<T> stream){
        if(stream.isEmptyStream()){
            return StreamInterface.makeEmptyStream();
        }

        R head = mapper.apply(stream.head);

        return new Stream.Builder<R>()
                .head(head)
                .process(new Process(()->map(mapper,stream.eval())))
                .build();
    }

    private Stream<T> filter(Predicate<T> predicate,Stream<T> stream){
        if(stream.isEmptyStream()){
            return StreamInterface.makeEmptyStream();
        }

        if(predicate.isOK(stream.head)){
            Stream<T> ok = new Stream.Builder<T>()
                    .head(stream.head)
                    .process(new Process(()->filter(predicate,stream.eval())))
                    .build();
            return ok;
        }else{
            Stream<T> not_ok = filter(predicate,stream.eval());
            return not_ok;
        }
    }

    private <R> R reduce(R initVal,Accumulate<R,T> accumulator,Stream<T> stream){
        if(stream.isEmptyStream()){
            return initVal;
        }

        T head = stream.head;
        R result = reduce(initVal,accumulator,stream.eval());

        // reduce实现
        return accumulator.apply(result,head);
    }

    private Stream<T> limit(int num,Stream<T> stream){
        if(num == 0){
            return StreamInterface.makeEmptyStream();
        }

        return new Stream.Builder<T>()
                .head(stream.head)
                .process(new Process(()->limit(num-1,stream.eval())))
                .build();
    }

    private void forEach(ForEach<T> consumer,Stream<T> stream){
        if(stream.isEmptyStream()){
            return;
        }

        consumer.apply(stream.head);
        forEach(consumer,stream.eval());
    }

    private <R, A> A collect(Collector<T, A, R> collector,Stream<T> stream){
        if(stream.isEmptyStream()){
            return collector.supplier().get();
        }

        T head = stream.head;
        A tail = collect(collector,stream.eval());

        return collector.accumulator().apply(tail,head);
    }

    private Stream<T> eval(){
        return this.process.eval();
    }

    private boolean isEmptyStream(){
        return this.isEnd;
    }
}