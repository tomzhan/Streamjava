package function;

import stream.Stream;

/**
 * @Author xiongyx
 * @Date 2019/3/6
 */
@FunctionalInterface
public interface EvalFunction<T> {

    Stream<T> apply();
}