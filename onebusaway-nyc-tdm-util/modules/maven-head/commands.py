# Here you can create play commands that are specific to the module, and extend existing commands
import os, inspect, shutil
import getopt
import sys
import subprocess

MODULE = 'maven'

# Commands that are specific to your module
COMMANDS = ['new','mvn:init','mvn:install','mvn:update','mvn:up','mvn:refresh','mvn:re','mvn:sources','mvn:src','mvn:play-dependency-sources','mvn:play-src']

HELP = {
  'update': "Updates libraries in the /lib folder",
  'refresh': "Deletes all *.jar and *.zip in /lib folder and updates libraries in the /lib folder",
}

##################################################
### execute
##################################################
def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    # Definition of Module path 
    module_dir = inspect.getfile(inspect.currentframe()).replace("commands.py","")
    kargs["module_dir"]=module_dir

    # Definition of Application path
    kargs["app_path"]=app.path

    if command == "mvn:install":
      callInstall(kargs)  

    if command == 'new' or command == "mvn:init":
      callInit(kargs)

    if command == 'mvn:update' or command == 'mvn:up':
      checkPomXML(app.path)
      callUpdate(kargs)

    if command == 'mvn:refresh' or command == 'mvn:re':
      checkPomXML(app.path)
      callRefresh(kargs)

    if command == 'mvn:sources' or command == 'mvn:src':
      checkPomXML(app.path)
      callSources(kargs)

    if command == 'mvn:play-dependency-sources' or command == 'mvn:play-src':
      checkPomXML(app.path)
      callPlaySources(kargs)      


##################################################
### mvn:install
##################################################
def callInstall(args):
      module_dir = args.get("module_dir")
      save_cwd = os.getcwd()
      print "~ Installing parent pom ..."
      print "~ "
      os.chdir(os.path.join(module_dir, 'resources/play-parent'))
      os.system('mvn clean install')
      os.chdir(save_cwd)      

##################################################
### mvn:init OR new
##################################################
def callInit(args):
      module_dir = args.get("module_dir")
      app_path=args.get("app_path")
      print "~ Executing mvn:init"
      print "~ "
      callInstall(args)
      if os.path.exists('pom.xml'):
          print "~ "
          print "~ Existing pom.xml will be backed up to pom.xml.bak"
          print "~ "
          shutil.copyfile('pom.xml', 'pom.xml.bak')
      print "~ "
      print "~ Copying pom.xml from module to project ..."
      print "~ "
      shutil.copyfile(os.path.join(module_dir,'resources/pom.xml'), os.path.join(app_path, 'pom.xml'))

##################################################
### mvn:update OR mvn:up
##################################################
def callUpdate(args):
      print "~"
      print "~ Retrieving dependencies..."
      print "~"
      os.system('mvn dependency:copy-dependencies')
      # callSources(args)

##################################################
### mvn:refresh OR mvn:re
##################################################
def callRefresh(args):
      print "~"
      print "~ Refresh dependencies..."
      print "~"
      os.system('mvn clean')
      callUpdate(args)

##################################################
### mvn:sources OR mvn:src
##################################################
def callSources(args):
      print "~"
      print "~ Retrieving dependencies sources..."
      print "~"
      os.system('mvn dependency:copy-dependencies -Dclassifier=sources')

##################################################
### mvn:play-dependency-sources OR mvn:play-src
##################################################
def callPlaySources(args):
      print "~"
      print "~ Retrieving Play's dependencies sources ..."
      print "~"
      os.system('mvn dependency:copy-dependencies -Pplay-src')

def checkPomXML(path):
      if not os.path.exists('pom.xml'):
        print "~ ERROR : pom.xml does not exist in your project."
        print "~ You can initialize a pom.xml with the command play mvn:init."
        sys.exit(-1)
