package com.alibaba.ttl.threadpool.agent;

import com.alibaba.ttl.threadpool.agent.internal.logging.Logger;
import com.alibaba.ttl.threadpool.agent.internal.transformlet.JavassistTransformlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * TTL {@link ClassFileTransformer} of Java Agent
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see ClassFileTransformer
 * @see <a href="https://docs.oracle.com/javase/10/docs/api/java/lang/instrument/package-summary.html">The mechanism for instrumentation</a>
 * @since 0.9.0
 */
public class TtlTransformer implements ClassFileTransformer {
    private static final Logger logger = Logger.getLogger(TtlTransformer.class);

    private static final byte[] EMPTY_BYTE_ARRAY = {};

    private final List<JavassistTransformlet> transformletList = new ArrayList<JavassistTransformlet>();

    TtlTransformer(List<? extends JavassistTransformlet> transformletList) {
        for (JavassistTransformlet transformlet : transformletList) {
            this.transformletList.add(transformlet);
            logger.info("[TtlTransformer] add Transformlet " + transformlet.getClass() + " success");
        }
    }

    @Override
    public final byte[] transform(@Nonnull final ClassLoader loader, @Nullable final String classFile, final Class<?> classBeingRedefined,
                                  final ProtectionDomain protectionDomain, final byte[] classFileBuffer) {
        try {
            // Lambda has no class file, no need to transform, just return.
            if (classFile == null) return EMPTY_BYTE_ARRAY;

            final String className = toClassName(classFile);
            for (JavassistTransformlet transformlet : transformletList) {
                final byte[] bytes = transformlet.doTransform(className, classFileBuffer, loader);
                if (bytes != null) return bytes;
            }
        } catch (Throwable t) {
            String msg = "Fail to transform class " + classFile + ", cause: " + t.toString();
            logger.log(Level.SEVERE, msg, t);
            throw new IllegalStateException(msg, t);
        }

        return EMPTY_BYTE_ARRAY;
    }

    private static String toClassName(final String classFile) {
        return classFile.replace('/', '.');
    }
}
