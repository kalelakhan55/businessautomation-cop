﻿


![Build for bcgithook](https://github.com/redhat-cop/businessautomation-cop/workflows/Build%20for%20bcgithook/badge.svg)

# bcgithook: Business Central git hooks in bash
Business Central is able to push changes into remote git repositories utilizing post-commit git hooks.
This project offers a bash-based implementation for such git hooks.

## Features
* Lightweight, relies only on standard git client and bash
* Works with any git provider, e.g. [GitLab](https://gitlab.com/), [GitHub](https://github.com/), [Bitbucket](https://bitbucket.org/), [Azure DevOps Repos](https://azure.microsoft.com/en-gb/services/devops/repos/), [Gitea](https://gitea.io/en-us/), [Gogs](https://gogs.io/), etc
* Can push to different git repository per project
* Supports run-of-the-mill git operations such as create a new project, create branch, commit/push changes to branches
* Works on Linux, Windows (on a Cygwin environment), probably on Mac (not tested)
* Scripted or manual installation mode
* Configurable logging of operations
* [Branch mapping](#branch-mapping), map branches from BC to remote Git repos

## Jump to

* [Configuration](#configuration)
	* [Commits per branch](#commits-per-branch)
	* [Branch Mapping](#branch-mapping)
	* [per-project configuration](#per-project-configuration)
	* [Using git-ssh style URLs for remote repositories](#using-git-ssh-style-urls-for-remote-repositories)
* [Installation](#installation)
* [Installation in OpenShift](#installation-in-openshift)
* [Notes on Git Repos](#notes-on-git-repos)
	* [GitLab](#GitLab)
	* [GitHub](#GitHub)
	* [Gitea](#Gitea)
	* [Azure DevOps](#azure-devops)
	* [Bitbucket](#bitbucket)
* [Compatability](#compatability)
* [Other implementations](#other-implementations)

## Configuration
**bcgithook** will look for its configuration in file `default.conf` placed in `$HOME/.bcgithook` directory. This file must be present even if per-project configuration files are used. 

The provided installation script will install the `default.conf` file in the `$JBOSS_HOME/git-hooks` directory as well. Configuration files in the `$JBOSS_HOME/git-hooks` directory take precedence over the files in the `$HOME/.bcgithook` directory.

The following variables need to be configured:

|Variable|Type|Content|
|--|--|--|
|`GIT_USER_NAME` | **required** | The ID for the git repo you are using. Surround the value in single quotation marks. |
|`GIT_PASSWD` | **required** | The password for the git repo. Surround the value in single quotation marks. |
|`GIT_URL` | **required** | The URL to connect to the git repo. See below for examples for various Git repos. Surround the value in single quotation marks.|
|`GIT_TYPE` | optional | Leave blank or undefined for all Git repos. Use **"azure"** (in quotation marks) for Azure DevOps |
|`LOG_LOCATION` | optional | The directory where logs should be written. Defaults to `$HOME` |
|`LOG_SYSTEM_REPOS` | optional | If set to "yes" will log access to system repositories, can result in some verbosity |
|`BRANCH_ALLOW`| optional | A comma-separated list, or a regular expression, of branches to allow commits to be pushed to.  When using a comma-separated list of branches, do not leave space between the comma and the branch name or enclose in quotes. |
|`BRANCH_ALLOW_REGEX`| optional | A regular expression specifying branches to allow commits for. If defined overrides `BRANCH_ALLOW`. Use quotes to specify a regular expression, for example `"^(feature/)\|^(hotfix)"` |
|`BRANCH_DENY`| optional | a comma-separated list of branches to deny commits to be pushed to. Do not leave space between the comma and the branch name or enclose in quotes. |
|`BRANCH_MAP`| optional | A comma-separated list of "SOURCE:TARGET" pairs that would map the source branch SOURCE to branch TARGET on the remote repo. Spaces between commas(,) or semi-colons(:) are trimmed. |

See below for example configurations for various Git repos.

### Commits per branch
Post-commit git hooks by default will push all commits in a branch to the configured remote git repository. This behaviour can be modified by declaring ALLOW and DENY lists.

ALLOW and DENY lists refer to branches that the post-commit git hooks will selectively push commits to according to the following rules:

* Both lists are optional. You can define either ALLOW or DENY, both or none.
* If present, ALLOW_REGEX will override ALLOW.
* If no list is defined post-commit git hooks will by default allow all commits to be pushed to the remote git repo.
* If only the ALLOW (or ALLOW_REGEX) list is defined, commits will be pushed to the remote git repo only for branches that can be found in this list.
* If only the DENY list is defined, commits to branches that can be found in this list will NOT be pushed to the remote git repo.
* If both ALLOW (or ALLOW_REGEX) and DENY lists are defined, then the DENY list takes precedence. If a branch can be found at both the ALLOW (or ALLOW_REGEX) and DENY lists, then commits to that branch will not be pushed to the remote git repo.

### Branch Mapping

By specifying the optional `BRANCH_MAP` variable it is possible to map one or more Business Central managed branches to branches to a remote git repository with a different name. For example, the following configuration:

```
BRANCH_MAP="damon:phintias, frodo:sam,  earth : moon , master:mainline"
```

would push commits from the local branches managed by Business Central `damon`, `frodo`, `earth` and `master` to branches in the remote git repository named `phintias`, `sam`, `moon` and `mainline` respectively. Useful for new projects created from within Business Central that have to conform with naming conventions imposed by a remote git repository.

Please note that there is currently no way of achieving the reverse. For example mapping of the remote branch `mainline` to a local one named `master` is not supported.


**Example 1: Separate branches in ALLOW and DENY lists**

| Definition | Expected Action
|-|-|
|`BRANCH_ALLOW=branch2,feature/fa`|Commits in branches "branch2" and "feature/fa" will be pushed to the remote git repo.|
|`BRANCH_DENY=master,release`|Commits in branches "master" and "release" will not be pushed to the remote git repo.|

**Example 2: Some branches in both ALLOW and DENY lists**

| Definition | Expected Action
|-|-|
|`BRANCH_ALLOW=branch2,feature/fa`|Commits in branch "feature/fa" will be pushed to the remote git repo. Branch "branch2" is also declared at `BRANCH_DENY` which takes precedence.|
|`BRANCH_DENY=master,release,branch2`|Commits in branches "master", "release" and "branch2" will not be pushed to the remote git repo.|

**Example 3: Separate branches in ALLOW and DENY lists, with regular expressions**

| Definition | Expected Action
|-|-|
|`BRANCH_ALLOW_REGEX="^(hotfix/)\|^(feature/)"`| Specifies branches to allow commits for in the form of regular expressions. In this example commits in branches starting with `hotfix/` or `feature/` will be allowed to be pushed to the remote git repo |
|`BRANCH_ALLOW="branch2,feature/fa"`| This variable is overridden by `BRANCH_ALLOW_REGEX` and will be ignored |
|`BRANCH_DENY=master,release`| Commits in branches "master" and "release" will not be pushed to the remote git repo.|

### per-project configuration

**bcgithook** allows for different configuration per-project. For this to happen a file with the same name as the project having the `.conf` suffix should be placed in the configuration directory. `default.conf` can be used as a template however only values that are different from `default.conf` need to be defined. For example, a project named "FormApplicationProcess" would use the `FormApplicationProcess.conf` configuration file if that file is found (or `formapplicationprocess.conf` depending on your git hosting conventions, please check against your case).

> Please follow case sensitivity rules for your operating system when naming configuration files.

There can be a single configuration directory (at `$HOME/.bcgithook`) for all instances of RHPAM/RHDM that are present in the same (physical or virtual) machine or a different directory per RHPAM/RHDM instance. In the latter case the configuration directory is at  `$JBOSS_HOME/git-hooks`.

Per project configuration files can also be placed at the `$JBOSS_HOME/git-hooks` directory. Configuration files placed in this directory take precedence over files in the `$HOME/.bcgithook` directory.

For new projects you can create the configuration beforehand so when BusinessCentral creates them the project specific configuration will be used automatically. That way different projects created in BusinessCentral can be associated to different repositories.

Please note that projects imported in Business Central will always be associated with the git repository they were imported from.

The `GIT_URL` used to specify the projects source can be in one of the following formats (in the following replace GitLab with your preferred git hostname):

| Format | Description
|-|-|
|`git@gitlab.com:<gitlab_id>`| **Generic** This is the same URL format that is used in the `default.conf` configuration. The project's name will be appended to it by the script. Can be re-used across projects. |
|`git@gitlab.com:<gitlab_id>/<project_name>.git`| **Specific** This format that includes the project's name can only be used in the per-project configuration file. Cannot be re-used in other projects. |

The same holds for HTTP(S) style URLs.

### Using git-ssh style URLs for remote repositories

bcgithook supports git-ssh URLs for remote git repos. These URLs are similar to `git@gitlab.com:<git_user_id>`. They can be used both in the default configuration as well as in the per-project configuration files.

Before configuring the git-ssh style URLs the key of the remote git repository must first be part of the `known_hosts` file for SSH. For GitLab this can be done by executing the following. Replace with the host of your remote git repository.

```
#
# Please continue reading for more secure ways of doing this
#
ssh-keyscan gitlab.com >> ~/.ssh/known_hosts
```

Going forward blindingly with this approach runs the risk of a potential MITM attack. It is advisable to check the SSH key obtained with the SSH fingerprints provided by git hosting providers. For GitLab and GitHub the relevant fingerprints could be obtained from the following locations:

* GitLab: https://docs.gitlab.com/ee/user/gitlab_com/
* Github: https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/githubs-ssh-key-fingerprints

To generate the fingerprint the following apprach could be used:

```
ssh-keyscan gitlab.com >> gitlabkey
ssh-keygen -lf gitlabkey
```



## Installation
Please execute the `install.sh` script providing the directory of your [JBoss EAP](https://developers.redhat.com/products/eap/overview) or [WildFly](https://wildfly.org/) installation (a.k.a `JBOSS_HOME`).

Example: `install.sh /opt/jboss/rhpam`

`install.sh JBOSS_HOME [global|local] [-h]`

Options:

* `-h` : will bring the help text
* `JBOSS_HOME` : the full path to your RHPAM installation (on JBoss EAP)
* `global|local`: specify the configuration directory, defaults to `global`
	* `global`: will install configuration files at the `$HOME/.bcgithook` directory
	* `local`: will install configuration files at the `JBOSS_HOME/git-hooks` directory

 The script assumes EAP standard directory layout and will perform the following steps:

> **IMPORTANT** : Please make sure that JBoss EAP or WildFly is not running before you execute following steps of run the `install.sh` script.

> If your installation does not follow standard directory layout, i.e. the result of extracting the EAP or WildFly ZIP file, please execute manually the steps outlined below

* Create default configuration in file `$HOME/.bcgithook/default.conf`. Missing directories will be created.
* Modify [JBoss EAP](https://developers.redhat.com/products/eap/overview) or [WildFly](https://wildfly.org/) configuration in `JBOSS_HOME/standalone/configuration/standalone.xml` by adding the following system property
```xml
<property name="org.uberfire.nio.git.hooks" value="${jboss.home.dir}/git-hooks"/>
```
* Create the `JBOSS_HOME/git-hooks` directory
* Copy the `scripts/post-commit.sh` script into the `JBOSS_HOME/git-hooks/post-commit` directory
* Modify the contents of **bcgithook** configuration in `$HOME/.bcgithook/default.conf` to match your needs before starting JBoss EAP or WildFly.

> **bcgithook** can be installed at anytime after Business Central is used, but post-commit git hooks will only be applied to projects created (or imported) after *bcgithook* installation

## Installation in OpenShift

By far the easier installation method is to utilize the `GIT_HOOKS_DIR` parameter. Have it pointing to a PV volume and manually copy the two files necessary to that volume.

* Documentation about the `GIT_HOOKS_DIR` parameter can be found at [RHPAM.7.10: Specifying the Git hooks directory for an authoring environment](https://access.redhat.com/documentation/en-us/red_hat_process_automation_manager/7.10/html-single/deploying_red_hat_process_automation_manager_on_red_hat_openshift_container_platform/index#template-deploy-githooksparams-openshift-templates-authoring-proc)

In the directory specified by that parameter place the `post-commit.sh` script renaming it to `post-commit` and the configuration file `default.conf.example` renaming it to `default.conf`. These two files are all that required for the git hooks. 

> **post-commit** remember to make this file executable by the user BC is running under
> **default.conf** remember to make this file readable by the user BC is running under. 

Having the `default.conf` in a PV allows for easy modification of the git-hooks configuration without having to reinstall everything. Also, due to BC invoking the post-commit git hook every time a commit is made the configuration can be changed without stopping BC, although this is not something you would want to do regularly.


## Notes on Git Repos
### GitLab
Pushing to GitLab works without any additional configuration.
When a project is created in Business Central it will be pushed to a same-named repository to Gitlab.

Example configuration:
```bash
GIT_TYPE=""
GIT_USER_NAME=gitlab_id
GIT_PASSWD=passwd
GIT_URL='https://gitlab.com/<gitlab_id>'
```
replace `gitlab_id` with your GitLab Id. Do not put a trailing `/` in the `GIT_URL`. By not specifying a specific project in `GIT_URL` you can reuse the configuration for multiple projects.

### GitHub
Create the repository to GitHub before trying to push to it. The repository should be created empty without README, license or `.gitginore` file. Once the repository is created at GitHub a project with the same name can be created at Business Central and it will be pushed to GitHub

Example configuration:
```bash
GIT_TYPE=""
GIT_USER_NAME=github_id
GIT_PASSWD=passwd
GIT_URL='https://github.com/<github_id>'
```
replace `github_id` with your GitHub Id. Do not put a trailing `/` in the `GIT_URL`. By not specifying a specific project in `GIT_URL` you can reuse the configuration for multiple projects.

No operation should be performed in the repository after it has been created. For example, do not create a file and delete it afterwards. Even if the repository looks empty the git version would have changed preventing Business Central from synchronizing with it.

### Gitea
For a localhost installation of Gitea `ENABLE_PUSH_CREATE_USER` must be set to `true` to allow the corresponding repository to be created at the time the project is created in Business Central.
Unless otherwise configured the file to be modified is `custom/conf/app.ini`
The [Gitea Cheat Sheet](https://docs.gitea.io/en-us/config-cheat-sheet/) provides additional guidance.

Example configuration:
```bash
GIT_TYPE=""
GIT_USER_NAME=gitea_id
GIT_PASSWD=passwd
GIT_URL='https://localhost:3000.com/<gitea_id>'
```
replace `gitea_id` with your Gitea Id. Do not put a trailing `/` in the `GIT_URL`. By not specifying a specific project in `GIT_URL` you can reuse the configuration for multiple projects.

### Azure DevOps
Create the repository in Azure DevOps before trying to push to it. The repository should be created empty without README, license or `.gitginore` file. Once the repository is created at Azure DevOps a project with the same name can be created at Business Central and it will be pushed to Azure DevOps

Example configuration:
```bash
GIT_TYPE="azure"
GIT_USER_NAME=azure_id
GIT_PASSWD=passwd
GIT_URL='https://<azure_id>@dev.azure.com/<azure_id>/<organisation>/_git'
```
`GIT_TYPE` should be set to **azure** for Azure DevOps. Leave blank or empty for all other types of Git repos.
Replace `azure_id` , `organisation` with the appropriate values. Do not put a trailing `/` in the `GIT_URL`. By not specifying a specific project in `GIT_URL` you can reuse the configuration for multiple projects.

No operation should be performed in the repository after it has been created. For example, do not create a file and delete it afterwards. Even if the repository looks empty the git version would have changed preventing Business Central from synchronizing with it.

### Bitbucket
Create the repository to Bitbucket before trying to push to it. The repository should be created empty without README, license or `.gitginore` file. Once the repository is created at Bitbucket a project with the same name can be created at Business Central and it will be pushed to Bitbucket

Example configuration:
```bash
GIT_TYPE=""
GIT_USER_NAME=bitbucket_id
GIT_PASSWD=passwd
GIT_URL='https://<bitbucket_id>@bitbucket.org/<bitbucket_id>'
```
replace `bitbucket_id` with your Bitbucket Id. Do not put a trailing `/` in the `GIT_URL`. By not specifying a specific project in `GIT_URL` you can reuse the configuration for multiple projects.

No operation should be performed in the repository after it has been created. For example, do not create a file and delete it afterwards. Even if the repository looks empty the git version would have changed preventing Business Central from synchronizing with it.

## Compatibility
**bcgithook** should be compatible with all versions of [RHPAM](https://developers.redhat.com/products/rhpam/overview), [jBPM](https://www.jbpm.org/) and [Drools](https://www.drools.org/) but it had only been tested with the following:
* RHPAM, versions 7.4, 7.4.1, 7.5, 7.6 and onwards to 7.12.0

## Other Implementations

Other implementations providing git hook support for Business Central are:
* [bc-git-integration-push](https://github.com/porcelli/bc-git-integration-push) Java based

## Links

* BA CoP Trello board: https://trello.com/c/3rhn7EyB

> Written with [StackEdit](https://stackedit.io/).
