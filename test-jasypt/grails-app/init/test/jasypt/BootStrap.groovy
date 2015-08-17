package test.jasypt

class BootStrap {

    def init = { servletContext ->
        println "Start"
    }
    def destroy = {
    }
}
