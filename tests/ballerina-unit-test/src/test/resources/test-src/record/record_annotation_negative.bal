import ballerina/http;

# Documentation for Test annotation
#
# + a - annotation `field a` documentation
# + a - annotation `field a` documentation
# + b - annotation `field b` documentation
# + c - annotation `field c` documentation
type Tst record {
    string a;
    string b;
    string cd;
};

annotation Test Tst;

# Documentation for testConst constant
#
# + testConstd - abc description
@final string testConsts = "TestConstantDocumentation";

# Documentation for Test struct
#
# + a - struct `field a` documentation
# + a - struct `field a` documentation
# + b - struct `field b` documentation
# + c - struct `field c` documentation
type Test record {
    int a;
    int b;
    int cdd;
};

# Gets a access parameter value (`true` or `false`) for a given key. Please note that #foo will always be bigger than #bar.
# Example:
# ``SymbolEnv pkgEnv = symbolEnter.packageEnvs.get(pkgNode.symbol);``
#
# + accessMode - read or write mode
# + accessMode - read or write mode
# + successfuls - boolean `true` or `false`
function File::open (string accessMode) returns (boolean) {
    boolean successful;
    return successful;
}

# Documentation for File object
#
# + path - file path. Example: ``C:\users\OddThinking\Documents\My Source\Widget\foo.src``
public type File object {

    public string path;

    public function open(string accessMode) returns boolean;
};

type Person record {
    string firstName;
    string lastName;
    int age;
    string city;
};

type Employee record {
    string name;
    int age;
    string address;
    any ageAny;
};

# Test Connector
#
# + url - url for endpoint
# + url - url for endpoint
# + urls - urls for endpoint
type TestConnector record {
  string url;
};

# PizzaService HTTP Service
#
# + conn - HTTP connection.
service<http:Service> PizzaService {

    # Check orderPizza resource.
    #
    # + req - In request.
    # + req - In request.
    # + reqest - In request.
//  # + conn - HTTP connection. Commented due to https://github.com/ballerina-lang/ballerina/issues/5586 issue

    orderPizza(endpoint conn, http:Request req) {
        http:Response res;
        _ = conn -> respond(res);
    }
}
