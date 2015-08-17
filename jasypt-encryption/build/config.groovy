
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        classNode.putNodeMetaData('projectVersion', '2.0.0-SNAPSHOT')
        classNode.putNodeMetaData('projectName', 'jasypt-encryption')
        classNode.putNodeMetaData('isPlugin', 'true')
    }
}
