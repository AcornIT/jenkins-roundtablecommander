package io.jenkins.plugins.roundtablecommander.RemoteConfig

f = namespace(lib.FormTagLib)
c = namespace(lib.CredentialsTagLib)

f.entry(title:_("Repository URL"), field:"url") {
    f.textbox(checkMethod: "post")
}

f.entry(title:_("Credentials"), field:"credentialsId") {
    c.select(onchange="""{
            var self = this.targetElement ? this.targetElement : this;
            var r = findPreviousFormItem(self,'url');
            r.onchange(r);
            self = null;
            r = null;
    }""")
}

f.entry(title:_("Name"), field:"name") {
    f.textbox()
}

f.entry(title:_("Workspaces to build"), field:"workspaces") {
    f.repeatableProperty(field:"workspaces", minimum:"1", add:_("Add Workspace"))
}


f.entry {
    div(class: "show-if-not-only") {
        f.repeatableDeleteButton()
    }
}