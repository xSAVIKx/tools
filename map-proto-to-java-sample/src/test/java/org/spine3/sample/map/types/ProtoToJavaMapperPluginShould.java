package org.spine3.sample.map.types;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.junit.Test;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.TypeUrl;
import org.spine3.type.ClassName;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Litus
 */
public class ProtoToJavaMapperPluginShould {

    private static final String PROTO_TYPE_PREFIX = "type.spine3.org/spine.sample.map.types.";
    private static final String JAVA_PACKAGE_PREFIX = "org.spine3.sample.map.types.";

    private static final String FIRST_MSG = "FirstMsg";
    private static final String SECOND_MSG = "SecondMsg";
    private static final String THIRD_MSG = "ThirdMsg";

    private static final String MSG_ONE = "Msg1";

    @Test
    public void put_entry_for_simple_message() {
        assertIsKnownType("SimpleMsg");
    }

    @Test
    public void put_entry_for_simple_enum() {
        assertIsKnownType("SimpleEnum");
    }

    @Test
    public void put_entry_for_msg_with_outer_class_set_in_protobuf_file_option() {
        assertIsKnownType("InnerMsg", "TestOuterClass$InnerMsg");
    }

    @Test
    public void put_entry_for_enum_with_outer_class_set_in_protobuf_file_option() {
        assertIsKnownType("InnerEnum", "TestOuterClass$InnerEnum");
    }

    @Test
    public void put_entry_for_msg_with_outer_class_as_file_name() {
        assertIsKnownType("TestMsg", "OuterClassName$TestMsg");
    }

    @Test
    public void put_entry_for_enum_with_outer_class_as_file_name() {
        assertIsKnownType("TestEnum", "OuterClassName$TestEnum");
    }

    @Test
    public void put_entry_for_top_level_messages() {
        assertIsKnownType(FIRST_MSG);
        assertIsKnownType(MSG_ONE);
    }

    @Test
    public void put_entry_for_second_level_messages() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG));
        assertIsKnownType(compose(MSG_ONE, "Msg2"));
    }

    @Test
    public void put_entry_for_second_level_enum() {
        assertIsKnownType(compose(FIRST_MSG, "SecondEnum"));
        assertIsKnownType(compose(MSG_ONE, "Enum2"));
    }

    @Test
    public void put_entry_for_third_level_msg() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_MSG));
    }

    @Test
    public void put_entry_for_third_level_enum() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, "ThirdEnum"));
    }

    @Test
    public void put_entry_for_fourth_level_msg() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_MSG, "FourthMsg"));
    }

    @Test
    public void put_entry_for_fourth_level_enum() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_MSG, "FourthEnum"));
    }

    private static void assertIsKnownType(String protoTypeName, String javaClassName) {
        final TypeUrl url = TypeUrl.of(PROTO_TYPE_PREFIX + protoTypeName);
        final ClassName className = KnownTypes.getClassName(url);

        assertEquals(JAVA_PACKAGE_PREFIX + javaClassName, className.value());
    }

    private static void assertIsKnownType(String typeName) {
        assertIsKnownType(typeName, typeName);
    }

    private static void assertIsKnownType(Iterable<String> parentsAndTypeName) {
        final String protoName = Joiner.on(".").join(parentsAndTypeName);
        final String javaName = Joiner.on("$").join(parentsAndTypeName);
        final TypeUrl url = TypeUrl.of(PROTO_TYPE_PREFIX + protoName);

        final ClassName className = KnownTypes.getClassName(url);

        assertEquals(JAVA_PACKAGE_PREFIX + javaName, className.value());
    }

    private static FluentIterable<String> compose(String... elems) {
        return FluentIterable.of(elems);
    }
}
