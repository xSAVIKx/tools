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
    private static final String FOURTH_MSG = "FourthMsg";

    private static final String SECOND_ENUM = "SecondEnum";
    private static final String THIRD_ENUM = "ThirdEnum";
    private static final String FOURTH_ENUM = "FourthEnum";

    private static final String MSG_ONE = "Msg1";
    private static final String MSG_TWO = "Msg2";
    private static final String ENUM_TWO = "Enum2";

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
        assertIsKnownType(compose(MSG_ONE, MSG_TWO));
    }

    @Test
    public void put_entry_for_second_level_enum() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_ENUM));
        assertIsKnownType(compose(MSG_ONE, ENUM_TWO));
    }

    @Test
    public void put_entry_for_third_level_msg() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_MSG));
    }

    @Test
    public void put_entry_for_third_level_enum() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_ENUM));
    }

    @Test
    public void put_entry_for_fourth_level_msg() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_MSG, FOURTH_MSG));
    }

    @Test
    public void put_entry_for_fourth_level_enum() {
        assertIsKnownType(compose(FIRST_MSG, SECOND_MSG, THIRD_MSG, FOURTH_ENUM));
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
        return FluentIterable.from(elems);
    }
}
