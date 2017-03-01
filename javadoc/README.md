##General
The module contains the custom Javadoc doclet, which excludes
elements annotated with `org.spine3.Internal`.  

##Usage
To use the doclet, specify the Javadoc options:

`javadoc -doclet org.spine3.tools.javadoc.ExcludeInternalDoclet -docletpath "classpathlist" ...`

##Tests
To test the tool, most tests run the tool programmatically for the specified sources,
then formed `RootDoc` is tested.

The sources used in the tests located in `resources` folder.
