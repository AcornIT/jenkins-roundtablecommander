package jenkins.plugins.roundtablecommander.WorkspaceSpec

f = namespace(lib.FormTagLib)

f.entry(title:_("Workspace Specifier (blank for 'any')"), field:"name") {
    f.textbox(default:"master")
}

f.entry(title:_("Shallow depth (0 for 'all history')"), field:"shallow") {
    f.textbox(default:"0")
}

f.entry {
    div() {
        f.repeatableDeleteButton()
    }
}