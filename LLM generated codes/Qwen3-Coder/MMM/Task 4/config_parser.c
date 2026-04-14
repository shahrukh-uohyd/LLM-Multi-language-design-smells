#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <ctype.h>
#include <math.h>
#include "com_example_ConfigParser.h"
#include "com_example_IniConfig.h"
#include "com_example_IniConfig_Section.h"
#include "com_example_JsonConfig.h"
#include "com_example_CsvTable.h"
#include "com_example_ConfigParseException.h"

// Forward declarations
static jobject parse_ini(JNIEnv *env, const char *data, size_t len);
static jobject parse_json(JNIEnv *env, const char *data, size_t len);
static jobject parse_csv(JNIEnv *env, const char *data, size_t len, jboolean hasHeader);
static jboolean validate_ini(JNIEnv *env, const char *data, size_t len);
static jboolean validate_json(JNIEnv *env, const char *data, size_t len);
static jboolean validate_csv(JNIEnv *env, const char *data, size_t len);

// Error reporting helper
static void throw_parse_exception(JNIEnv *env, const char *message, int line, int column, const char *context) {
    jclass exClass = (*env)->FindClass(env, "com/example/ConfigParseException");
    if (!exClass) return; // Exception already pending
    
    jmethodID constructor = (*env)->GetMethodID(env, exClass, "<init>", 
        "(Ljava/lang/String;IILjava/lang/String;)V");
    if (!constructor) return;
    
    jstring msgStr = (*env)->NewStringUTF(env, message);
    jstring contextStr = context ? (*env)->NewStringUTF(env, context) : NULL;
    
    if (!msgStr) return;
    if (context && !contextStr) return;
    
    (*env)->ThrowNew(env, exClass, "Failed to create exception");
    
    jobject ex = (*env)->NewObject(env, exClass, constructor, 
        msgStr, line, column, contextStr ? contextStr : (*env)->NewStringUTF(env, ""));
    if (ex) {
        (*env)->Throw(env, ex);
    }
}

// ===== INI PARSER =====

typedef struct {
    char *key;
    char *value;
    char *comment;
} ini_entry_t;

typedef struct {
    char *name;
    ini_entry_t *entries;
    size_t entry_count;
    size_t entry_capacity;
} ini_section_t;

typedef struct {
    ini_section_t *sections;
    size_t section_count;
    size_t section_capacity;
    ini_section_t global_section;
} ini_config_t;

static void ini_config_init(ini_config_t *cfg) {
    memset(cfg, 0, sizeof(ini_config_t));
    cfg->global_section.name = NULL;
    cfg->global_section.entries = NULL;
    cfg->global_section.entry_count = 0;
    cfg->global_section.entry_capacity = 0;
}

static void ini_config_free(ini_config_t *cfg) {
    for (size_t i = 0; i < cfg->section_count; i++) {
        ini_section_t *sec = &cfg->sections[i];
        free(sec->name);
        for (size_t j = 0; j < sec->entry_count; j++) {
            free(sec->entries[j].key);
            free(sec->entries[j].value);
            free(sec->entries[j].comment);
        }
        free(sec->entries);
    }
    free(cfg->sections);
    
    // Free global section
    for (size_t j = 0; j < cfg->global_section.entry_count; j++) {
        free(cfg->global_section.entries[j].key);
        free(cfg->global_section.entries[j].value);
        free(cfg->global_section.entries[j].comment);
    }
    free(cfg->global_section.entries);
}

static bool ini_add_section(ini_config_t *cfg, const char *name) {
    if (cfg->section_count >= cfg->section_capacity) {
        size_t new_cap = cfg->section_capacity * 2 + 8;
        ini_section_t *new_secs = realloc(cfg->sections, new_cap * sizeof(ini_section_t));
        if (!new_secs) return false;
        cfg->sections = new_secs;
        cfg->section_capacity = new_cap;
    }
    
    ini_section_t *sec = &cfg->sections[cfg->section_count++];
    sec->name = name ? strdup(name) : NULL;
    sec->entries = NULL;
    sec->entry_count = 0;
    sec->entry_capacity = 0;
    return true;
}

static bool ini_add_entry(ini_section_t *sec, const char *key, const char *value, const char *comment) {
    if (sec->entry_count >= sec->entry_capacity) {
        size_t new_cap = sec->entry_capacity * 2 + 8;
        ini_entry_t *new_entries = realloc(sec->entries, new_cap * sizeof(ini_entry_t));
        if (!new_entries) return false;
        sec->entries = new_entries;
        sec->entry_capacity = new_cap;
    }
    
    ini_entry_t *entry = &sec->entries[sec->entry_count++];
    entry->key = key ? strdup(key) : NULL;
    entry->value = value ? strdup(value) : NULL;
    entry->comment = comment ? strdup(comment) : NULL;
    return true;
}

static char* trim_quotes(char *str) {
    size_t len = strlen(str);
    if (len >= 2 && ((str[0] == '"' && str[len-1] == '"') || (str[0] == '\'' && str[len-1] == '\''))) {
        str[len-1] = '\0';
        return str + 1;
    }
    return str;
}

static char* skip_whitespace(const char *p) {
    while (*p && isspace((unsigned char)*p)) p++;
    return (char*)p;
}

static char* parse_ini_value(const char *start, const char **end_ptr) {
    const char *p = start;
    bool in_quotes = false;
    char quote_char = '\0';
    
    while (*p) {
        if (*p == '#' || *p == ';') {
            if (!in_quotes) break; // Comment outside quotes
        }
        
        if (*p == '"' || *p == '\'') {
            if (!in_quotes) {
                in_quotes = true;
                quote_char = *p;
            } else if (*p == quote_char) {
                in_quotes = false;
            }
        }
        
        if (*p == '\n' && !in_quotes) break;
        p++;
    }
    
    size_t len = p - start;
    while (len > 0 && isspace((unsigned char)start[len-1])) len--;
    
    char *result = malloc(len + 1);
    if (!result) return NULL;
    strncpy(result, start, len);
    result[len] = '\0';
    
    *end_ptr = p;
    return result;
}

static jobject parse_ini(JNIEnv *env, const char *data, size_t len) {
    ini_config_t cfg;
    ini_config_init(&cfg);
    ini_add_section(&cfg, NULL); // Global section
    
    const char *p = data;
    int line = 1;
    ini_section_t *current_section = &cfg.global_section;
    char line_buf[1024];
    
    while (*p) {
        // Skip whitespace and comments
        if (*p == '#' || *p == ';') {
            while (*p && *p != '\n') p++;
            if (*p == '\n') { p++; line++; }
            continue;
        }
        
        if (isspace((unsigned char)*p)) {
            if (*p == '\n') line++;
            p++;
            continue;
        }
        
        // Section header: [section]
        if (*p == '[') {
            p++;
            const char *section_start = p;
            while (*p && *p != ']' && *p != '\n') p++;
            
            if (*p != ']') {
                snprintf(line_buf, sizeof(line_buf), "Unclosed section header starting at: %.50s", section_start);
                throw_parse_exception(env, "Missing closing bracket in section header", line, (int)(p - data), line_buf);
                ini_config_free(&cfg);
                return NULL;
            }
            
            size_t section_len = p - section_start;
            while (section_len > 0 && isspace((unsigned char)section_start[section_len-1])) section_len--;
            
            char *section_name = malloc(section_len + 1);
            if (!section_name) {
                ini_config_free(&cfg);
                return NULL;
            }
            strncpy(section_name, section_start, section_len);
            section_name[section_len] = '\0';
            
            if (!ini_add_section(&cfg, section_name)) {
                free(section_name);
                ini_config_free(&cfg);
                return NULL;
            }
            current_section = &cfg.sections[cfg.section_count - 1];
            free(section_name);
            
            p++; // Skip ']'
            while (*p && *p != '\n') p++; // Skip rest of line
            if (*p == '\n') { p++; line++; }
            continue;
        }
        
        // Key-value pair: key = value
        const char *key_start = p;
        while (*p && *p != '=' && *p != '\n') p++;
        
        if (*p != '=') {
            snprintf(line_buf, sizeof(line_buf), "Expected '=' after key: %.50s", key_start);
            throw_parse_exception(env, "Invalid key-value pair (missing '=')", line, (int)(p - data), line_buf);
            ini_config_free(&cfg);
            return NULL;
        }
        
        size_t key_len = p - key_start;
        while (key_len > 0 && isspace((unsigned char)key_start[key_len-1])) key_len--;
        
        char *key = malloc(key_len + 1);
        if (!key) {
            ini_config_free(&cfg);
            return NULL;
        }
        strncpy(key, key_start, key_len);
        key[key_len] = '\0';
        
        p++; // Skip '='
        p = skip_whitespace(p);
        
        const char *value_end;
        char *value = parse_ini_value(p, &value_end);
        if (!value) {
            free(key);
            ini_config_free(&cfg);
            return NULL;
        }
        
        // Extract inline comment if present
        char *comment = NULL;
        const char *comment_start = value_end;
        while (*comment_start && isspace((unsigned char)*comment_start)) comment_start++;
        
        if (*comment_start == '#' || *comment_start == ';') {
            comment_start++;
            while (*comment_start && isspace((unsigned char)*comment_start)) comment_start++;
            
            size_t comment_len = 0;
            const char *c = comment_start;
            while (*c && *c != '\n') { comment_len++; c++; }
            
            comment = malloc(comment_len + 1);
            if (comment) {
                strncpy(comment, comment_start, comment_len);
                comment[comment_len] = '\0';
            }
        }
        
        // Add entry to current section
        if (!ini_add_entry(current_section, key, value, comment)) {
            free(key);
            free(value);
            free(comment);
            ini_config_free(&cfg);
            return NULL;
        }
        
        free(key);
        free(value);
        free(comment);
        
        p = value_end;
        while (*p && *p != '\n') p++;
        if (*p == '\n') { p++; line++; }
    }
    
    // Build Java objects
    jclass sectionClass = (*env)->FindClass(env, "com/example/IniConfig$Section");
    jclass mapClass = (*env)->FindClass(env, "java/util/HashMap");
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    
    if (!sectionClass || !mapClass || !stringClass) {
        ini_config_free(&cfg);
        return NULL;
    }
    
    jmethodID mapInit = (*env)->GetMethodID(env, mapClass, "<init>", "()V");
    jmethodID mapPut = (*env)->GetMethodID(env, mapClass, "put", 
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    
    // Create sections map
    jobject sectionsMap = (*env)->NewObject(env, mapClass, mapInit);
    if (!sectionsMap) {
        ini_config_free(&cfg);
        return NULL;
    }
    
    // Helper to create Section object from ini_section_t
    auto create_section_obj = [&](ini_section_t *sec) -> jobject {
        jobject valuesMap = (*env)->NewObject(env, mapClass, mapInit);
        jobject commentsMap = (*env)->NewObject(env, mapClass, mapInit);
        
        for (size_t i = 0; i < sec->entry_count; i++) {
            ini_entry_t *entry = &sec->entries[i];
            if (entry->key) {
                jstring jkey = (*env)->NewStringUTF(env, entry->key);
                jstring jval = entry->value ? (*env)->NewStringUTF(env, entry->value) : NULL;
                (*env)->CallObjectMethod(env, valuesMap, mapPut, jkey, jval ? jval : (*env)->NewStringUTF(env, ""));
                
                if (entry->comment) {
                    jstring jcomment = (*env)->NewStringUTF(env, entry->comment);
                    (*env)->CallObjectMethod(env, commentsMap, mapPut, jkey, jcomment);
                }
            }
        }
        
        jmethodID secInit = (*env)->GetMethodID(env, sectionClass, "<init>", 
            "(Ljava/util/Map;Ljava/util/Map;)V");
        return (*env)->NewObject(env, sectionClass, secInit, valuesMap, commentsMap);
    };
    
    // Add named sections to map
    for (size_t i = 0; i < cfg.section_count; i++) {
        ini_section_t *sec = &cfg.sections[i];
        if (sec->name) {
            jstring jname = (*env)->NewStringUTF(env, sec->name);
            jobject secObj = create_section_obj(sec);
            (*env)->CallObjectMethod(env, sectionsMap, mapPut, jname, secObj);
        }
    }
    
    // Create global section
    jobject globalSection = create_section_obj(&cfg.global_section);
    
    // Create IniConfig object
    jclass iniClass = (*env)->FindClass(env, "com/example/IniConfig");
    jmethodID iniInit = (*env)->GetMethodID(env, iniClass, "<init>", 
        "(Ljava/util/Map;Lcom/example/IniConfig$Section;)V");
    jobject result = (*env)->NewObject(env, iniClass, iniInit, sectionsMap, globalSection);
    
    ini_config_free(&cfg);
    return result;
}

// ===== JSON PARSER (Simplified but robust) =====

typedef enum {
    JSON_NULL,
    JSON_BOOL,
    JSON_NUMBER,
    JSON_STRING,
    JSON_OBJECT,
    JSON_ARRAY
} json_type_t;

typedef struct json_value_t {
    json_type_t type;
    union {
        bool bool_value;
        double num_value;
        char *str_value;
        struct {
            char **keys;
            struct json_value_t **values;
            size_t count;
        } obj_value;
        struct {
            struct json_value_t **values;
            size_t count;
        } arr_value;
    } u;
} json_value_t;

static void json_free(json_value_t *val) {
    if (!val) return;
    
    switch (val->type) {
        case JSON_STRING:
            free(val->u.str_value);
            break;
        case JSON_OBJECT:
            for (size_t i = 0; i < val->u.obj_value.count; i++) {
                free(val->u.obj_value.keys[i]);
                json_free(val->u.obj_value.values[i]);
            }
            free(val->u.obj_value.keys);
            free(val->u.obj_value.values);
            break;
        case JSON_ARRAY:
            for (size_t i = 0; i < val->u.arr_value.count; i++) {
                json_free(val->u.arr_value.values[i]);
            }
            free(val->u.arr_value.values);
            break;
        default:
            break;
    }
    free(val);
}

static json_value_t* parse_json_value(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size);
static json_value_t* parse_json_string(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size);
static json_value_t* parse_json_number(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size);

static json_value_t* parse_json_object(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size) {
    json_value_t *obj = calloc(1, sizeof(json_value_t));
    if (!obj) return NULL;
    obj->type = JSON_OBJECT;
    
    (*pp)++; // Skip '{'
    (*col)++;
    
    while (**pp) {
        if (**pp == '}') {
            (*pp)++;
            (*col)++;
            return obj;
        }
        
        // Skip whitespace
        while (**pp && isspace((unsigned char)**pp)) {
            if (**pp == '\n') { (*line)++; *col = 0; }
            else (*col)++;
            (*pp)++;
        }
        
        if (**pp != '"') {
            snprintf(context, context_size, "Expected quoted key, got '%c'", **pp);
            throw_parse_exception(env, "Invalid JSON: expected key string", *line, *col, context);
            json_free(obj);
            return NULL;
        }
        
        json_value_t *key_val = parse_json_string(env, pp, line, col, context, context_size);
        if (!key_val) {
            json_free(obj);
            return NULL;
        }
        
        // Skip whitespace
        while (**pp && isspace((unsigned char)**pp)) {
            if (**pp == '\n') { (*line)++; *col = 0; }
            else (*col)++;
            (*pp)++;
        }
        
        if (**pp != ':') {
            snprintf(context, context_size, "Expected ':', got '%c'", **pp);
            throw_parse_exception(env, "Invalid JSON: expected ':' after key", *line, *col, context);
            json_free(key_val);
            json_free(obj);
            return NULL;
        }
        (*pp)++;
        (*col)++;
        
        // Skip whitespace
        while (**pp && isspace((unsigned char)**pp)) {
            if (**pp == '\n') { (*line)++; *col = 0; }
            else (*col)++;
            (*pp)++;
        }
        
        json_value_t *val = parse_json_value(env, pp, line, col, context, context_size);
        if (!val) {
            json_free(key_val);
            json_free(obj);
            return NULL;
        }
        
        // Add to object
        obj->u.obj_value.keys = realloc(obj->u.obj_value.keys, 
            (obj->u.obj_value.count + 1) * sizeof(char*));
        obj->u.obj_value.values = realloc(obj->u.obj_value.values, 
            (obj->u.obj_value.count + 1) * sizeof(json_value_t*));
        
        if (!obj->u.obj_value.keys || !obj->u.obj_value.values) {
            json_free(key_val);
            json_free(val);
            json_free(obj);
            return NULL;
        }
        
        obj->u.obj_value.keys[obj->u.obj_value.count] = key_val->u.str_value;
        obj->u.obj_value.values[obj->u.obj_value.count] = val;
        obj->u.obj_value.count++;
        free(key_val); // Only free container, not the string
        
        // Skip whitespace
        while (**pp && isspace((unsigned char)**pp)) {
            if (**pp == '\n') { (*line)++; *col = 0; }
            else (*col)++;
            (*pp)++;
        }
        
        if (**pp == ',') {
            (*pp)++;
            (*col)++;
        } else if (**pp != '}') {
            snprintf(context, context_size, "Expected ',' or '}', got '%c'", **pp);
            throw_parse_exception(env, "Invalid JSON: expected ',' or '}'", *line, *col, context);
            json_free(obj);
            return NULL;
        }
    }
    
    snprintf(context, context_size, "Unclosed object");
    throw_parse_exception(env, "Invalid JSON: unclosed object", *line, *col, context);
    json_free(obj);
    return NULL;
}

static json_value_t* parse_json_array(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size) {
    json_value_t *arr = calloc(1, sizeof(json_value_t));
    if (!arr) return NULL;
    arr->type = JSON_ARRAY;
    
    (*pp)++; // Skip '['
    (*col)++;
    
    while (**pp) {
        if (**pp == ']') {
            (*pp)++;
            (*col)++;
            return arr;
        }
        
        // Skip whitespace
        while (**pp && isspace((unsigned char)**pp)) {
            if (**pp == '\n') { (*line)++; *col = 0; }
            else (*col)++;
            (*pp)++;
        }
        
        json_value_t *val = parse_json_value(env, pp, line, col, context, context_size);
        if (!val) {
            json_free(arr);
            return NULL;
        }
        
        // Add to array
        arr->u.arr_value.values = realloc(arr->u.arr_value.values, 
            (arr->u.arr_value.count + 1) * sizeof(json_value_t*));
        if (!arr->u.arr_value.values) {
            json_free(val);
            json_free(arr);
            return NULL;
        }
        arr->u.arr_value.values[arr->u.arr_value.count++] = val;
        
        // Skip whitespace
        while (**pp && isspace((unsigned char)**pp)) {
            if (**pp == '\n') { (*line)++; *col = 0; }
            else (*col)++;
            (*pp)++;
        }
        
        if (**pp == ',') {
            (*pp)++;
            (*col)++;
        } else if (**pp != ']') {
            snprintf(context, context_size, "Expected ',' or ']', got '%c'", **pp);
            throw_parse_exception(env, "Invalid JSON: expected ',' or ']'", *line, *col, context);
            json_free(arr);
            return NULL;
        }
    }
    
    snprintf(context, context_size, "Unclosed array");
    throw_parse_exception(env, "Invalid JSON: unclosed array", *line, *col, context);
    json_free(arr);
    return NULL;
}

static json_value_t* parse_json_value(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size) {
    // Skip whitespace
    while (**pp && isspace((unsigned char)**pp)) {
        if (**pp == '\n') { (*line)++; *col = 0; }
        else (*col)++;
        (*pp)++;
    }
    
    switch (**pp) {
        case 'n':
            if (strncmp(*pp, "null", 4) == 0) {
                json_value_t *val = calloc(1, sizeof(json_value_t));
                if (!val) return NULL;
                val->type = JSON_NULL;
                *pp += 4;
                *col += 4;
                return val;
            }
            break;
        case 't':
            if (strncmp(*pp, "true", 4) == 0) {
                json_value_t *val = calloc(1, sizeof(json_value_t));
                if (!val) return NULL;
                val->type = JSON_BOOL;
                val->u.bool_value = true;
                *pp += 4;
                *col += 4;
                return val;
            }
            break;
        case 'f':
            if (strncmp(*pp, "false", 5) == 0) {
                json_value_t *val = calloc(1, sizeof(json_value_t));
                if (!val) return NULL;
                val->type = JSON_BOOL;
                val->u.bool_value = false;
                *pp += 5;
                *col += 5;
                return val;
            }
            break;
        case '"':
            return parse_json_string(env, pp, line, col, context, context_size);
        case '{':
            return parse_json_object(env, pp, line, col, context, context_size);
        case '[':
            return parse_json_array(env, pp, line, col, context, context_size);
        case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9':
            return parse_json_number(env, pp, line, col, context, context_size);
    }
    
    snprintf(context, context_size, "Unexpected character '%c'", **pp);
    throw_parse_exception(env, "Invalid JSON value", *line, *col, context);
    return NULL;
}

static json_value_t* parse_json_string(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size) {
    (*pp)++; // Skip opening quote
    (*col)++;
    
    const char *start = *pp;
    size_t len = 0;
    bool escaped = false;
    
    while (**pp) {
        if (escaped) {
            escaped = false;
        } else if (**pp == '\\') {
            escaped = true;
        } else if (**pp == '"') {
            break;
        } else if (**pp == '\n') {
            (*line)++;
            *col = 0;
        } else {
            (*col)++;
        }
        len++;
        (*pp)++;
    }
    
    if (**pp != '"') {
        snprintf(context, context_size, "Unclosed string starting at: %.30s", start);
        throw_parse_exception(env, "Invalid JSON: unclosed string", *line, *col, context);
        return NULL;
    }
    
    // Allocate and copy unescaped string
    char *result = malloc(len + 1);
    if (!result) return NULL;
    
    const char *src = start;
    char *dst = result;
    escaped = false;
    
    while (src < *pp) {
        if (escaped) {
            switch (*src) {
                case '"': *dst++ = '"'; break;
                case '\\': *dst++ = '\\'; break;
                case '/': *dst++ = '/'; break;
                case 'b': *dst++ = '\b'; break;
                case 'f': *dst++ = '\f'; break;
                case 'n': *dst++ = '\n'; break;
                case 'r': *dst++ = '\r'; break;
                case 't': *dst++ = '\t'; break;
                case 'u': // Unicode escape (simplified)
                    *dst++ = '?'; // Placeholder for proper Unicode handling
                    src += 4; // Skip 4 hex digits
                    break;
                default: *dst++ = *src; break;
            }
            escaped = false;
        } else if (*src == '\\') {
            escaped = true;
        } else {
            *dst++ = *src;
        }
        src++;
    }
    *dst = '\0';
    
    (*pp)++; // Skip closing quote
    (*col)++;
    
    json_value_t *val = calloc(1, sizeof(json_value_t));
    if (!val) {
        free(result);
        return NULL;
    }
    val->type = JSON_STRING;
    val->u.str_value = result;
    return val;
}

static json_value_t* parse_json_number(JNIEnv *env, const char **pp, int *line, int *col, char *context, size_t context_size) {
    const char *start = *pp;
    bool has_dot = false;
    bool has_e = false;
    
    while (**pp) {
        if (**pp == '.' && !has_dot) {
            has_dot = true;
        } else if ((**pp == 'e' || **pp == 'E') && !has_e) {
            has_e = true;
            if ((*pp)[1] == '+' || (*pp)[1] == '-') (*pp)++;
        } else if (!isdigit((unsigned char)**pp) && **pp != '-' && **pp != '+') {
            break;
        }
        (*pp)++;
        (*col)++;
    }
    
    if (*pp == start) {
        snprintf(context, context_size, "Invalid number format");
        throw_parse_exception(env, "Invalid JSON number", *line, *col, context);
        return NULL;
    }
    
    char *endptr;
    double value = strtod(start, &endptr);
    
    json_value_t *val = calloc(1, sizeof(json_value_t));
    if (!val) return NULL;
    val->type = JSON_NUMBER;
    val->u.num_value = value;
    return val;
}

static jobject build_json_object(JNIEnv *env, json_value_t *val);

static jobject build_json_value(JNIEnv *env, json_value_t *val) {
    if (!val) return NULL;
    
    jclass jsonClass = (*env)->FindClass(env, "com/example/JsonConfig");
    if (!jsonClass) return NULL;
    
    switch (val->type) {
        case JSON_NULL:
            return (*env)->GetStaticObjectField(env, jsonClass, 
                (*env)->GetStaticFieldID(env, jsonClass, "NULL", "Lcom/example/JsonConfig;"));
        case JSON_BOOL:
            return (*env)->GetStaticObjectField(env, jsonClass, 
                val->u.bool_value ? 
                (*env)->GetStaticFieldID(env, jsonClass, "TRUE", "Lcom/example/JsonConfig;") :
                (*env)->GetStaticFieldID(env, jsonClass, "FALSE", "Lcom/example/JsonConfig;"));
        case JSON_NUMBER:
            return (*env)->CallStaticObjectMethod(env, jsonClass, 
                (*env)->GetStaticMethodID(env, jsonClass, "numberValue", "(D)Lcom/example/JsonConfig;"),
                val->u.num_value);
        case JSON_STRING:
            return (*env)->CallStaticObjectMethod(env, jsonClass, 
                (*env)->GetStaticMethodID(env, jsonClass, "stringValue", "(Ljava/lang/String;)Lcom/example/JsonConfig;"),
                (*env)->NewStringUTF(env, val->u.str_value));
        case JSON_OBJECT:
            return build_json_object(env, val);
        case JSON_ARRAY: {
            jclass listClass = (*env)->FindClass(env, "java/util/ArrayList");
            jmethodID listInit = (*env)->GetMethodID(env, listClass, "<init>", "()V");
            jmethodID listAdd = (*env)->GetMethodID(env, listClass, "add", "(Ljava/lang/Object;)Z");
            
            jobject list = (*env)->NewObject(env, listClass, listInit);
            for (size_t i = 0; i < val->u.arr_value.count; i++) {
                jobject item = build_json_value(env, val->u.arr_value.values[i]);
                (*env)->CallBooleanMethod(env, list, listAdd, item);
            }
            
            return (*env)->CallStaticObjectMethod(env, jsonClass, 
                (*env)->GetStaticMethodID(env, jsonClass, "arrayValue", "(Ljava/util/List;)Lcom/example/JsonConfig;"),
                list);
        }
    }
    return NULL;
}

static jobject build_json_object(JNIEnv *env, json_value_t *val) {
    jclass mapClass = (*env)->FindClass(env, "java/util/LinkedHashMap");
    jmethodID mapInit = (*env)->GetMethodID(env, mapClass, "<init>", "()V");
    jmethodID mapPut = (*env)->GetMethodID(env, mapClass, "put", 
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    
    jobject map = (*env)->NewObject(env, mapClass, mapInit);
    for (size_t i = 0; i < val->u.obj_value.count; i++) {
        jstring key = (*env)->NewStringUTF(env, val->u.obj_value.keys[i]);
        jobject value = build_json_value(env, val->u.obj_value.values[i]);
        (*env)->CallObjectMethod(env, map, mapPut, key, value);
    }
    
    jclass jsonClass = (*env)->FindClass(env, "com/example/JsonConfig");
    return (*env)->CallStaticObjectMethod(env, jsonClass, 
        (*env)->GetStaticMethodID(env, jsonClass, "objectValue", "(Ljava/util/Map;)Lcom/example/JsonConfig;"),
        map);
}

static jobject parse_json(JNIEnv *env, const char *data, size_t len) {
    const char *p = data;
    int line = 1, col = 0;
    char context[256];
    
    json_value_t *root = parse_json_value(env, &p, &line, &col, context, sizeof(context));
    if (!root) return NULL;
    
    jobject result = build_json_value(env, root);
    json_free(root);
    return result;
}

// ===== CSV PARSER =====

static jobject parse_csv(JNIEnv *env, const char *data, size_t len, jboolean hasHeader) {
    // Detect delimiter (comma, tab, semicolon)
    char delimiter = ',';
    size_t comma_count = 0, tab_count = 0, semicolon_count = 0;
    
    for (size_t i = 0; i < len && i < 1024; i++) {
        if (data[i] == ',') comma_count++;
        else if (data[i] == '\t') tab_count++;
        else if (data[i] == ';') semicolon_count++;
    }
    
    if (tab_count > comma_count && tab_count > semicolon_count) delimiter = '\t';
    else if (semicolon_count > comma_count) delimiter = ';';
    
    // First pass: count rows and columns
    size_t row_count = 0, max_cols = 0;
    size_t col_count = 0;
    bool in_quotes = false;
    
    for (size_t i = 0; i <= len; i++) {
        char c = (i < len) ? data[i] : '\n';
        
        if (c == '"') {
            in_quotes = !in_quotes;
        } else if (c == '\n' && !in_quotes) {
            if (col_count > 0 || i > 0) { // Non-empty line
                if (col_count > max_cols) max_cols = col_count;
                row_count++;
            }
            col_count = 0;
        } else if (c == delimiter && !in_quotes) {
            col_count++;
        }
    }
    
    if (col_count > 0) {
        if (col_count + 1 > max_cols) max_cols = col_count + 1;
        row_count++;
    }
    
    if (max_cols == 0 || row_count == 0) {
        throw_parse_exception(env, "Empty CSV data", 1, 0, NULL);
        return NULL;
    }
    
    if (hasHeader == JNI_TRUE) {
        if (row_count < 2) {
            throw_parse_exception(env, "CSV has header but no data rows", 1, 0, NULL);
            return NULL;
        }
        row_count--; // Exclude header from data rows
    }
    
    // Allocate result structures
    char ***cells = calloc(row_count + (hasHeader == JNI_TRUE ? 1 : 0), sizeof(char**));
    if (!cells) return NULL;
    
    for (size_t i = 0; i < row_count + (hasHeader == JNI_TRUE ? 1 : 0); i++) {
        cells[i] = calloc(max_cols, sizeof(char*));
    }
    
    // Second pass: parse cells
    size_t row_idx = 0, col_idx = 0;
    size_t cell_start = 0;
    in_quotes = false;
    bool escaped_quote = false;
    
    for (size_t i = 0; i <= len; i++) {
        char c = (i < len) ? data[i] : '\n';
        bool end_of_cell = false;
        bool end_of_row = false;
        
        if (c == '"') {
            if (escaped_quote) {
                escaped_quote = false;
            } else if (in_quotes) {
                // Check for escaped quote ("")
                if (i + 1 < len && data[i + 1] == '"') {
                    escaped_quote = true;
                } else {
                    in_quotes = false;
                }
            } else {
                in_quotes = true;
                cell_start = i + 1;
            }
        } else if (c == delimiter && !in_quotes) {
            end_of_cell = true;
        } else if (c == '\n' && !in_quotes) {
            end_of_cell = true;
            end_of_row = true;
        }
        
        if (end_of_cell) {
            size_t cell_len = i - cell_start;
            if (cell_len > 0 && data[cell_start] == '"' && data[i-1] == '"') {
                cell_start++;
                cell_len -= 2;
            }
            
            // Unescape quotes ("") -> "
            char *cell = malloc(cell_len + 1);
            if (cell) {
                size_t dst = 0;
                for (size_t src = 0; src < cell_len; src++) {
                    if (src + 1 < cell_len && data[cell_start + src] == '"' && data[cell_start + src + 1] == '"') {
                        cell[dst++] = '"';
                        src++;
                    } else {
                        cell[dst++] = data[cell_start + src];
                    }
                }
                cell[dst] = '\0';
                cells[row_idx][col_idx] = cell;
            }
            
            col_idx++;
            cell_start = i + 1;
        }
        
        if (end_of_row) {
            if (col_idx > 0 || i > 0) {
                row_idx++;
            }
            col_idx = 0;
            cell_start = i + 1;
        }
    }
    
    // Build Java objects
    jclass csvClass = (*env)->FindClass(env, "com/example/CsvTable");
    jmethodID csvInit = (*env)->GetMethodID(env, csvClass, "<init>", 
        "([Ljava/lang/String;[[Ljava/lang/Object;C)V");
    
    // Create headers array
    jobjectArray headers = NULL;
    if (hasHeader == JNI_TRUE && cells[0]) {
        headers = (*env)->NewObjectArray(env, max_cols, (*env)->FindClass(env, "java/lang/String"), NULL);
        for (size_t i = 0; i < max_cols && cells[0][i]; i++) {
            (*env)->SetObjectArrayElement(env, headers, i, (*env)->NewStringUTF(env, cells[0][i]));
        }
    }
    
    // Create rows array with type inference
    jobjectArray rows = (*env)->NewObjectArray(env, row_count, 
        (*env)->FindClass(env, "[Ljava/lang/Object;"), NULL);
    
    for (size_t r = 0; r < row_count; r++) {
        size_t src_row = (hasHeader == JNI_TRUE) ? r + 1 : r;
        jobjectArray row = (*env)->NewObjectArray(env, max_cols, 
            (*env)->FindClass(env, "java/lang/Object"), NULL);
        
        for (size_t c = 0; c < max_cols && cells[src_row][c]; c++) {
            char *cell = cells[src_row][c];
            jobject cellObj = NULL;
            
            // Type inference
            if (strcmp(cell, "true") == 0 || strcmp(cell, "TRUE") == 0 || 
                strcmp(cell, "yes") == 0 || strcmp(cell, "YES") == 0) {
                jclass boolClass = (*env)->FindClass(env, "java/lang/Boolean");
                jmethodID boolValueOf = (*env)->GetStaticMethodID(env, boolClass, "valueOf", "(Z)Ljava/lang/Boolean;");
                cellObj = (*env)->CallStaticObjectMethod(env, boolClass, boolValueOf, JNI_TRUE);
            } else if (strcmp(cell, "false") == 0 || strcmp(cell, "FALSE") == 0 || 
                       strcmp(cell, "no") == 0 || strcmp(cell, "NO") == 0) {
                jclass boolClass = (*env)->FindClass(env, "java/lang/Boolean");
                jmethodID boolValueOf = (*env)->GetStaticMethodID(env, boolClass, "valueOf", "(Z)Ljava/lang/Boolean;");
                cellObj = (*env)->CallStaticObjectMethod(env, boolClass, boolValueOf, JNI_FALSE);
            } else {
                // Try parsing as integer
                char *endptr;
                long int_val = strtol(cell, &endptr, 10);
                if (*endptr == '\0' && cell != endptr) {
                    jclass intClass = (*env)->FindClass(env, "java/lang/Integer");
                    jmethodID intValueOf = (*env)->GetStaticMethodID(env, intClass, "valueOf", "(I)Ljava/lang/Integer;");
                    cellObj = (*env)->CallStaticObjectMethod(env, intClass, intValueOf, (jint)int_val);
                } else {
                    // Try parsing as double
                    double double_val = strtod(cell, &endptr);
                    if (*endptr == '\0' && cell != endptr) {
                        jclass doubleClass = (*env)->FindClass(env, "java/lang/Double");
                        jmethodID doubleValueOf = (*env)->GetStaticMethodID(env, doubleClass, "valueOf", "(D)Ljava/lang/Double;");
                        cellObj = (*env)->CallStaticObjectMethod(env, doubleClass, doubleValueOf, double_val);
                    } else {
                        // Fallback to string
                        cellObj = (*env)->NewStringUTF(env, cell);
                    }
                }
            }
            
            (*env)->SetObjectArrayElement(env, row, c, cellObj);
        }
        
        (*env)->SetObjectArrayElement(env, rows, r, row);
    }
    
    // Clean up native allocations
    for (size_t i = 0; i < row_count + (hasHeader == JNI_TRUE ? 1 : 0); i++) {
        if (cells[i]) {
            for (size_t j = 0; j < max_cols; j++) {
                free(cells[i][j]);
            }
            free(cells[i]);
        }
    }
    free(cells);
    
    return (*env)->NewObject(env, csvClass, csvInit, headers, rows, delimiter);
}

// ===== VALIDATION FUNCTIONS (Simplified) =====

static jboolean validate_ini(JNIEnv *env, const char *data, size_t len) {
    // Basic validation: check for unclosed quotes and sections
    bool in_quotes = false;
    char quote_char = '\0';
    bool in_section = false;
    
    for (size_t i = 0; i < len; i++) {
        if (data[i] == '"' || data[i] == '\'') {
            if (!in_quotes) {
                in_quotes = true;
                quote_char = data[i];
            } else if (data[i] == quote_char) {
                in_quotes = false;
            }
        } else if (data[i] == '[' && !in_quotes) {
            if (in_section) return JNI_FALSE; // Nested section
            in_section = true;
        } else if (data[i] == ']' && !in_quotes) {
            if (!in_section) return JNI_FALSE; // Unopened section
            in_section = false;
        } else if (data[i] == '\n') {
            in_quotes = false; // Reset quotes at line end (simplified)
        }
    }
    
    return (in_quotes || in_section) ? JNI_FALSE : JNI_TRUE;
}

static jboolean validate_json(JNIEnv *env, const char *data, size_t len) {
    // Simplified validation using bracket/brace counting
    int braces = 0, brackets = 0, quotes = 0;
    bool in_quotes = false;
    char escape_next = 0;
    
    for (size_t i = 0; i < len; i++) {
        if (escape_next) {
            escape_next = 0;
            continue;
        }
        
        if (data[i] == '\\') {
            escape_next = 1;
            continue;
        }
        
        if (data[i] == '"') {
            in_quotes = !in_quotes;
            if (!in_quotes) quotes--;
            else quotes++;
        } else if (!in_quotes) {
            if (data[i] == '{') braces++;
            else if (data[i] == '}') braces--;
            else if (data[i] == '[') brackets++;
            else if (data[i] == ']') brackets--;
        }
        
        if (braces < 0 || brackets < 0) return JNI_FALSE;
    }
    
    return (braces == 0 && brackets == 0 && quotes == 0) ? JNI_TRUE : JNI_FALSE;
}

static jboolean validate_csv(JNIEnv *env, const char *data, size_t len) {
    // Basic validation: check for consistent columns
    char delimiter = ',';
    if (memchr(data, '\t', len)) delimiter = '\t';
    else if (memchr(data, ';', len)) delimiter = ';';
    
    size_t expected_cols = 0;
    size_t current_cols = 0;
    bool in_quotes = false;
    
    for (size_t i = 0; i <= len; i++) {
        char c = (i < len) ? data[i] : '\n';
        
        if (c == '"') {
            in_quotes = !in_quotes;
        } else if (c == delimiter && !in_quotes) {
            current_cols++;
        } else if (c == '\n' && !in_quotes) {
            if (expected