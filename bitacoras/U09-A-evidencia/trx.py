import sys, xml.etree.ElementTree as E
n = "{http://microsoft.com/schemas/VisualStudio/TeamTest/2010}"
r = E.parse(sys.argv[1]).getroot()
c = r.find(f".//{n}Counters").attrib
print(" ".join(f"{k}={c[k]}" for k in ("total", "executed", "passed", "failed", "notExecuted")))
for t in sorted(r.iter(f"{n}UnitTestResult"), key=lambda t: t.get("testName")):
    print(("FALLA " if t.get("outcome") != "Passed" else "") + t.get("testName").rsplit(".", 1)[-1])
