/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openejb.core.cmp.cmp2;

import org.apache.xbean.asm8.ClassWriter;
import org.apache.xbean.asm8.FieldVisitor;
import org.apache.xbean.asm8.MethodVisitor;
import org.apache.xbean.asm8.Opcodes;
import org.apache.xbean.asm8.Type;

/**
 * Class for generating a class file that implements
 * CMP1 type of persistence.
 */
public class Cmp1Generator implements Opcodes {
    private final String implClassName;
    private final String beanClassName;
    private final ClassWriter cw;
    private boolean unknownPk;
    private final PostCreateGenerator postCreateGenerator;

    /**
     * Constructor for a CMP1 class generator.
     *
     * @param cmpImplClass The name of the generated implementation class.
     * @param beanClass    The source Bean class we're wrappering.
     */
    public Cmp1Generator(final String cmpImplClass, final Class beanClass) {
        beanClassName = Type.getInternalName(beanClass);
        implClassName = cmpImplClass.replace('.', '/');

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        postCreateGenerator = new PostCreateGenerator(beanClass, cw);
    }

    /**
     * Generate the class for implementing CMP 1 level of
     * persistence.
     *
     * @return The generated byte-array containing the class data.
     */
    public byte[] generate() {
        // We're creating a superclass for the implementation.  We force this to implement
        // EntityBean to allow POJOs to be used as the bean class. 
        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, implClassName, null, beanClassName, new String[]{"javax/ejb/EntityBean"});

        // if we have an unknown pk, we need to add a field for the pk
        if (unknownPk) {
            // public Long OpenEJB_pk;
            final FieldVisitor fv = cw.visitField(ACC_PUBLIC, "OpenEJB_pk", "Ljava/lang/Long;", null, null);
            fv.visitEnd();
        }

        // there's not much to generate here.  We create a default constructor, then generate the 
        // post create methods.  A lot of the work is done by having mapped superclass information that 
        // we pass to the JPA engine. 
        createConstructor();
        postCreateGenerator.generate();

        cw.visitEnd();

        return cw.toByteArray();
    }

    /**
     * Create a default constructor for our bean super
     * class.  This is just a forwarding constructor that
     * calls the superclass (the bean implementation class)
     * default constructor.
     */
    private void createConstructor() {
        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, beanClassName, "<init>", "()V", false);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isUnknownPk() {
        return unknownPk;
    }

    public void setUnknownPk(final boolean unknownPk) {
        this.unknownPk = unknownPk;
    }
}
