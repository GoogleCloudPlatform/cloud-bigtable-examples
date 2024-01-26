import nox

DEFAULT_PYTHON_VERSION = "3.11"
BLACK_VERSION = "black==22.3.0"
LINT_PATHS = ["system", "noxfile.py"]


@nox.session(python=DEFAULT_PYTHON_VERSION)
def tests(session):
    # Install dependencies
    session.install("-r", "requirements.txt")

    # Run your integration tests
    session.run("pytest", "-s", "system/workflow-test.py")


@nox.session(python=DEFAULT_PYTHON_VERSION)
def blacken(session):
    """Run black. Format code to uniform standard."""
    session.install(BLACK_VERSION)
    session.run(
        "black",
        *LINT_PATHS,
    )
