# Copyright 2012 the V8 project authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'variables': {
    'v8_code': 1,
    'v8_random_seed%': 314159265,
    'v8_vector_stores%': 0,
    'embed_script%': "",
    'warmup_script%': "",
    'v8_extra_library_files%': [],
    'v8_experimental_extra_library_files%': [],
    'mksnapshot_exec': '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)mksnapshot<(EXECUTABLE_SUFFIX)',
    'v8_os_page_size%': 0,
  },
  'includes': ['toolchain.gypi', 'features.gypi', 'inspector.gypi'],
  'targets': [
    {
      'target_name': 'v8',
      'dependencies_traverse': 1,
      'dependencies': ['v8_maybe_snapshot', 'v8_dump_build_config#target'],
      'conditions': [
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
        }, {
          'toolsets': ['target'],
        }],
        ['component=="shared_library"', {
          'type': '<(component)',
          'sources': [
            # Note: on non-Windows we still build this file so that gyp
            # has some sources to link into the component.
            '../src/v8dll-main.cc',
          ],
          'include_dirs': [
            '..',
          ],
          'defines': [
            'BUILDING_V8_SHARED',
          ],
          'direct_dependent_settings': {
            'defines': [
              'USING_V8_SHARED',
            ],
          },
          'conditions': [
            ['OS=="mac"', {
              'xcode_settings': {
                'OTHER_LDFLAGS': ['-dynamiclib', '-all_load']
              },
            }],
            ['soname_version!=""', {
              'product_extension': 'so.<(soname_version)',
            }],
          ],
        },
        {
          'type': 'none',
        }],
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '../include/',
        ],
      },
    },
    {
      # This rule delegates to either v8_snapshot, v8_nosnapshot, or
      # v8_external_snapshot, depending on the current variables.
      # The intention is to make the 'calling' rules a bit simpler.
      'target_name': 'v8_maybe_snapshot',
      'type': 'none',
      'conditions': [
        ['v8_use_snapshot!="true"', {
          # The dependency on v8_base should come from a transitive
          # dependency however the Android toolchain requires libv8_base.a
          # to appear before libv8_snapshot.a so it's listed explicitly.
          'dependencies': ['v8_base', 'v8_nosnapshot'],
        }],
        ['v8_use_snapshot=="true" and v8_use_external_startup_data==0', {
          # The dependency on v8_base should come from a transitive
          # dependency however the Android toolchain requires libv8_base.a
          # to appear before libv8_snapshot.a so it's listed explicitly.
          'dependencies': ['v8_base', 'v8_snapshot'],
        }],
        ['v8_use_snapshot=="true" and v8_use_external_startup_data==1 and want_separate_host_toolset==0', {
          'dependencies': ['v8_base', 'v8_external_snapshot'],
          'inputs': [ '<(PRODUCT_DIR)/snapshot_blob.bin', ],
        }],
        ['v8_use_snapshot=="true" and v8_use_external_startup_data==1 and want_separate_host_toolset==1', {
          'dependencies': ['v8_base', 'v8_external_snapshot'],
          'target_conditions': [
            ['_toolset=="host"', {
              'inputs': [
                '<(PRODUCT_DIR)/snapshot_blob_host.bin',
              ],
            }, {
              'inputs': [
                '<(PRODUCT_DIR)/snapshot_blob.bin',
              ],
            }],
          ],
        }],
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
        }, {
          'toolsets': ['target'],
        }],
      ]
    },
    {
      'target_name': 'v8_init',
      'type': 'static_library',
      'dependencies': [
        'v8_initializers',
        'v8_torque#host',
      ],
      'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
        '../include/',
      ],
      'sources': [
        '../src/setup-isolate-full.cc',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtin-definitions-from-dsl.h',
      ],
      'conditions': [
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
        }, {
          'toolsets': ['target'],
        }],
      ],
    },
    {
      'target_name': 'v8_initializers',
      'type': 'static_library',
      'dependencies': [
        'v8_base',
        'v8_torque#host',
      ],
       'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
        '../include/',
      ],
      'sources': [
        '../src/builtins/builtins-arguments-gen.cc',
        '../src/builtins/builtins-arguments-gen.h',
        '../src/builtins/builtins-array-gen.cc',
        '../src/builtins/builtins-array-gen.h',
        '../src/builtins/builtins-async-function-gen.cc',
        '../src/builtins/builtins-async-gen.cc',
        '../src/builtins/builtins-async-gen.h',
        '../src/builtins/builtins-async-generator-gen.cc',
        '../src/builtins/builtins-async-iterator-gen.cc',
        '../src/builtins/builtins-boolean-gen.cc',
        '../src/builtins/builtins-call-gen.cc',
        '../src/builtins/builtins-call-gen.h',
        '../src/builtins/builtins-collections-gen.cc',
        '../src/builtins/builtins-console-gen.cc',
        '../src/builtins/builtins-constructor-gen.cc',
        '../src/builtins/builtins-constructor-gen.h',
        '../src/builtins/builtins-constructor.h',
        '../src/builtins/builtins-conversion-gen.cc',
        '../src/builtins/builtins-date-gen.cc',
        '../src/builtins/builtins-debug-gen.cc',
        '../src/builtins/builtins-function-gen.cc',
        '../src/builtins/builtins-generator-gen.cc',
        '../src/builtins/builtins-global-gen.cc',
        '../src/builtins/builtins-handler-gen.cc',
        '../src/builtins/builtins-ic-gen.cc',
        '../src/builtins/builtins-internal-gen.cc',
        '../src/builtins/builtins-interpreter-gen.cc',
        '../src/builtins/builtins-intl-gen.cc',
        '../src/builtins/builtins-iterator-gen.h',
        '../src/builtins/builtins-iterator-gen.cc',
        '../src/builtins/builtins-math-gen.cc',
        '../src/builtins/builtins-math-gen.h',
        '../src/builtins/builtins-number-gen.cc',
        '../src/builtins/builtins-object-gen.cc',
        '../src/builtins/builtins-promise-gen.cc',
        '../src/builtins/builtins-promise-gen.h',
        '../src/builtins/builtins-proxy-gen.cc',
        '../src/builtins/builtins-proxy-gen.h',
        '../src/builtins/builtins-reflect-gen.cc',
        '../src/builtins/builtins-regexp-gen.cc',
        '../src/builtins/builtins-regexp-gen.h',
        '../src/builtins/builtins-sharedarraybuffer-gen.cc',
        '../src/builtins/builtins-string-gen.cc',
        '../src/builtins/builtins-string-gen.h',
        '../src/builtins/builtins-symbol-gen.cc',
        '../src/builtins/builtins-typed-array-gen.cc',
        '../src/builtins/builtins-typed-array-gen.h',
        '../src/builtins/builtins-utils-gen.h',
        '../src/builtins/builtins-wasm-gen.cc',
        '../src/builtins/growable-fixed-array-gen.cc',
        '../src/builtins/growable-fixed-array-gen.h',
        '../src/builtins/setup-builtins-internal.cc',
        '../src/heap/setup-heap-internal.cc',
        '../src/ic/accessor-assembler.cc',
        '../src/ic/accessor-assembler.h',
        '../src/ic/binary-op-assembler.cc',
        '../src/ic/binary-op-assembler.h',
        '../src/ic/keyed-store-generic.cc',
        '../src/ic/keyed-store-generic.h',
        '../src/interpreter/interpreter-assembler.cc',
        '../src/interpreter/interpreter-assembler.h',
        '../src/interpreter/interpreter-generator.cc',
        '../src/interpreter/interpreter-generator.h',
        '../src/interpreter/interpreter-intrinsics-generator.cc',
        '../src/interpreter/interpreter-intrinsics-generator.h',
        '../src/interpreter/setup-interpreter-internal.cc',
        '../src/interpreter/setup-interpreter.h',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-array-from-dsl-gen.cc',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-array-from-dsl-gen.h',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-base-from-dsl-gen.cc',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-base-from-dsl-gen.h',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-typed-array-from-dsl-gen.cc',
        '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-typed-array-from-dsl-gen.h',
      ],
      'conditions': [
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
        }, {
          'toolsets': ['target'],
        }],
        ['v8_target_arch=="ia32"', {
          'sources': [
            '../src/builtins/ia32/builtins-ia32.cc',
          ],
        }],
        ['v8_target_arch=="x64"', {
          'sources': [
            '../src/builtins/x64/builtins-x64.cc',
          ],
        }],
        ['v8_target_arch=="arm"', {
          'sources': [
            '../src/builtins/arm/builtins-arm.cc',
          ],
        }],
        ['v8_target_arch=="arm64"', {
          'sources': [
            '../src/builtins/arm64/builtins-arm64.cc',
          ],
        }],
        ['v8_target_arch=="mips" or v8_target_arch=="mipsel"', {
          'sources': [
            '../src/builtins/mips/builtins-mips.cc',
          ],
        }],
        ['v8_target_arch=="mips64" or v8_target_arch=="mips64el"', {
          'sources': [
            '../src/builtins/mips64/builtins-mips64.cc',
          ],
        }],
        ['v8_target_arch=="ppc" or v8_target_arch=="ppc64"', {
          'sources': [
            '../src/builtins/ppc/builtins-ppc.cc',
          ],
        }],
        ['v8_target_arch=="s390" or v8_target_arch=="s390x"', {
          'sources': [
            '../src/builtins/s390/builtins-s390.cc',
          ],
        }],
        ['v8_enable_i18n_support==0', {
          'sources!': [
            '../src/builtins/builtins-intl-gen.cc',
          ],
        }],
      ],
    },
    {
      'target_name': 'v8_snapshot',
      'type': 'static_library',
      'conditions': [
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
          'dependencies': [
            'mksnapshot#host',
            'js2c#host',
          ],
        }, {
          'toolsets': ['target'],
          'dependencies': [
            'mksnapshot',
            'js2c',
          ],
        }],
        ['component=="shared_library"', {
          'defines': [
            'BUILDING_V8_SHARED',
          ],
          'direct_dependent_settings': {
            'defines': [
              'USING_V8_SHARED',
            ],
          },
        }],
      ],
      'dependencies': [
        'v8_base',
      ],
      'include_dirs+': [
        '..',
        '<(DEPTH)',
      ],
      'sources': [],
      'actions': [
        {
          'action_name': 'run_mksnapshot',
          'inputs': [
            '<(mksnapshot_exec)',
          ],
          'conditions': [
            ['embed_script!=""', {
              'inputs': [
                '<(embed_script)',
              ],
            }],
            ['warmup_script!=""', {
              'inputs': [
                '<(warmup_script)',
              ],
            }],
          ],
          'outputs': [
            '<(INTERMEDIATE_DIR)/snapshot.cc',
          ],
          'variables': {
            'mksnapshot_flags': [],
            'conditions': [
              ['v8_random_seed!=0', {
                'mksnapshot_flags': ['--random-seed', '<(v8_random_seed)'],
              }],
              ['v8_vector_stores!=0', {
                'mksnapshot_flags': ['--vector-stores'],
              }],
            ],
          },
          'action': [
            '<(mksnapshot_exec)',
            '<@(mksnapshot_flags)',
            '--startup_src', '<@(INTERMEDIATE_DIR)/snapshot.cc',
            '<(embed_script)',
            '<(warmup_script)',
          ],
        },
      ],
    },
    {
      'target_name': 'v8_nosnapshot',
      'type': 'none', # 'type': 'static_library',
      'dependencies': [
        'v8_base',
      ],
      'include_dirs+': [
        '..',
        '<(DEPTH)',
      ],
      'sources': [],
      'conditions': []
    },
    {
      'target_name': 'v8_external_snapshot',
      'type': 'static_library',
      'conditions': [
        [ 'v8_use_external_startup_data==1', {
          'conditions': [
            ['want_separate_host_toolset==1', {
              'toolsets': ['host', 'target'],
              'dependencies': [
                'mksnapshot#host',
                'js2c#host',
                'natives_blob',
            ]}, {
              'toolsets': ['target'],
              'dependencies': [
                'mksnapshot',
                'js2c',
                'natives_blob',
              ],
            }],
            ['component=="shared_library"', {
              'defines': [
                'BUILDING_V8_SHARED',
              ],
              'direct_dependent_settings': {
                'defines': [
                  'USING_V8_SHARED',
                ],
              },
            }],
          ],
          'dependencies': [
            'v8_base',
          ],
          'include_dirs+': [
            '..',
            '<(DEPTH)',
          ],
          'sources': [
            '../src/setup-isolate-deserialize.cc',
            '../src/snapshot/natives-external.cc',
            '../src/snapshot/snapshot-external.cc',
          ],
          'actions': [
            {
              'action_name': 'run_mksnapshot (external)',
              'inputs': [
                '<(mksnapshot_exec)',
              ],
              'variables': {
                'mksnapshot_flags': [],
                'conditions': [
                  ['v8_random_seed!=0', {
                    'mksnapshot_flags': ['--random-seed', '<(v8_random_seed)'],
                  }],
                  ['v8_vector_stores!=0', {
                    'mksnapshot_flags': ['--vector-stores'],
                  }],
                  ['v8_os_page_size!=0', {
                    'mksnapshot_flags': ['--v8_os_page_size', '<(v8_os_page_size)'],
                  }],
                ],
              },
              'conditions': [
                ['embed_script!=""', {
                  'inputs': [
                    '<(embed_script)',
                  ],
                }],
                ['warmup_script!=""', {
                  'inputs': [
                    '<(warmup_script)',
                  ],
                }],
                ['want_separate_host_toolset==1', {
                  'target_conditions': [
                    ['_toolset=="host"', {
                      'outputs': [
                        '<(PRODUCT_DIR)/snapshot_blob_host.bin',
                      ],
                      'action': [
                        '<(mksnapshot_exec)',
                        '<@(mksnapshot_flags)',
                        '--startup_blob', '<(PRODUCT_DIR)/snapshot_blob_host.bin',
                        '<(embed_script)',
                        '<(warmup_script)',
                      ],
                    }, {
                      'outputs': [
                        '<(PRODUCT_DIR)/snapshot_blob.bin',
                      ],
                      'action': [
                        '<(mksnapshot_exec)',
                        '<@(mksnapshot_flags)',
                        '--startup_blob', '<(PRODUCT_DIR)/snapshot_blob.bin',
                        '<(embed_script)',
                        '<(warmup_script)',
                      ],
                    }],
                  ],
                }, {
                  'outputs': [
                    '<(PRODUCT_DIR)/snapshot_blob.bin',
                  ],
                  'action': [
                    '<(mksnapshot_exec)',
                    '<@(mksnapshot_flags)',
                    '--startup_blob', '<(PRODUCT_DIR)/snapshot_blob.bin',
                    '<(embed_script)',
                    '<(warmup_script)',
                  ],
                }],
              ],
            },
          ],
        }],
      ],
    },
    {
      'target_name': 'v8_base',
      'type': 'static_library',
      'dependencies': [
        'v8_libbase',
        'v8_libsampler',
      ],
      'direct_dependent_settings': {
        'include_dirs+': ['<(SHARED_INTERMEDIATE_DIR)'],
      },
      'objs': ['foo.o'],
      'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
        '../../uv/include',
        '<(graalvm)/include/',
        '<(DEPTH)',
        '<(SHARED_INTERMEDIATE_DIR)'
      ],
      'conditions': [
        ['OS=="linux"', {
          'include_dirs+': [
            '<(graalvm)/include/linux/',
          ],
        }],
        ['OS=="solaris"', {
          'include_dirs+': [
            '<(graalvm)/include/solaris/',
          ],
        }],
        ['OS=="mac"', {
          'include_dirs+': [
            '<(graalvm)/include/darwin/',
          ],
        }],
      ],
      'link_settings': {
        'conditions' : [
          ['OS=="linux" or OS=="solaris"', {
            'libraries': [
              "-Wl,-rpath='$$ORIGIN/../../../../lib/'",
              "-Wl,-rpath='$$ORIGIN/../../../../jre/lib/'",
              "-Wl,-rpath='$$ORIGIN/../../../../jre/languages/R/lib/'",
            ],
          }],
          ['OS=="linux" and target_arch=="x64"', {
            'libraries': [
              '-L<(graalvm)/jre/lib/amd64/server -L<(graalvm)/jre/lib/amd64',
              "-Wl,-rpath='$$ORIGIN/../../../../lib/amd64/'",
              "-Wl,-rpath='$$ORIGIN/../../../../jre/lib/amd64/'",
            ],
          }],
          ['OS=="solaris" or (OS=="linux" and target_arch=="sparcv9")', {
            'libraries': [
              '-L<(graalvm)/jre/lib/sparcv9/server -L<(graalvm)/jre/lib/sparcv9',
              "-Wl,-rpath='$$ORIGIN/../../../../lib/sparcv9/'",
              "-Wl,-rpath='$$ORIGIN/../../../../jre/lib/sparcv9/'",
            ],
          }],
          ['OS=="mac"', {
            'libraries': [
              '-L<(graalvm)/jre/lib/server -L<(graalvm)/jre/lib',
              "-Wl,-rpath,'@loader_path/../../../../lib/'",
              "-Wl,-rpath,'@loader_path/../../../../jre/lib/'",
              "-Wl,-rpath,'@loader_path/../../../../jre/languages/R/lib/'",
            ],
          }],
          ['1 == 1', {
            'libraries': [
              '-ljsig',
              '-ldl',
            ],
          }],
       ]},
      'sources': [
        '../include/v8-debug.h',
        '../include/v8-platform.h',
        '../include/v8-profiler.h',
        '../include/v8-testing.h',
        '../include/v8-util.h',
        '../include/v8-value-serializer-version.h',
        '../include/v8-version-string.h',
        '../include/v8-version.h',
        '../include/v8.h',
        '../include/v8config.h',
        '../src/graal/callbacks.cc',
        '../src/graal/graal_array.cc',
        '../src/graal/graal_array_buffer.cc',
        '../src/graal/graal_array_buffer_view.cc',
        '../src/graal/graal_big_int.cc',
        '../src/graal/graal_boolean.cc',
        '../src/graal/graal_context.cc',
        '../src/graal/graal_data.cc',
        '../src/graal/graal_date.cc',
        '../src/graal/graal_external.cc',
        '../src/graal/graal_function.cc',
        '../src/graal/graal_function_callback_arguments.cc',
        '../src/graal/graal_function_callback_info.cc',
        '../src/graal/graal_function_template.cc',
        '../src/graal/graal_handle_content.cc',
        '../src/graal/graal_isolate.cc',
        '../src/graal/graal_map.cc',
        '../src/graal/graal_message.cc',
        '../src/graal/graal_missing_primitive.cc',
        '../src/graal/graal_module.cc',
        '../src/graal/graal_name.cc',
        '../src/graal/graal_number.cc',
        '../src/graal/graal_object.cc',
        '../src/graal/graal_object_template.cc',
        '../src/graal/graal_primitive.cc',
        '../src/graal/graal_promise.cc',
        '../src/graal/graal_property_callback_info.cc',
        '../src/graal/graal_proxy.cc',
        '../src/graal/graal_regexp.cc',
        '../src/graal/graal_script.cc',
        '../src/graal/graal_set.cc',
        '../src/graal/graal_stack_frame.cc',
        '../src/graal/graal_stack_trace.cc',
        '../src/graal/graal_string.cc',
        '../src/graal/graal_symbol.cc',
        '../src/graal/graal_template.cc',
        '../src/graal/graal_unbound_script.cc',
        '../src/graal/graal_value.cc',
        '../src/graal/v8.cc'
      ],
    },
    {
      'target_name': 'v8_libbase',
      'type': 'none',
      'toolsets': ['host', 'target'],
      'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
      ],
      'direct_dependent_settings': {
        'include_dirs+': ['..'],
      },
      'sources': [
      ],
    },
    {
      'target_name': 'v8_libplatform',
      'type': 'none',
      'variables': {
        'optimize': 'max',
      },
      'dependencies': [
        'v8_libbase',
      ],
      'include_dirs+': [
        '..',
        '<(DEPTH)',
        '../include/',
      ],
      'sources': [
        '../include/libplatform/libplatform.h',
        '../include/libplatform/libplatform-export.h',
        '../include/libplatform/v8-tracing.h',
        'libplatform/default-platform.h',
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '../include/',
        ],
      },
    },
    {
      'target_name': 'v8_libsampler',
      'type': 'none',
      'variables': {
        'optimize': 'max',
      },
      'dependencies': [
        'v8_libbase',
      ],
      'include_dirs+': [
        '..',
        '../include/',
      ],
      'sources': [
      ],
      'conditions': [
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
        }, {
          'toolsets': ['target'],
        }],
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '../include/',
        ],
      },
    },
    {
      'target_name': 'natives_blob',
      'type': 'none',
    },
    {
      'target_name': 'js2c',
      'type': 'none',
      'conditions': [],
      'variables': {},
      'actions': [],
    },
    {
      'target_name': 'torque',
      'type': 'executable',
      'toolsets': ['host'],
      'dependencies': ['v8_libbase'],
      'cflags_cc!': ['-fno-exceptions', '-fno-rtti'],
      'xcode_settings': {
        'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',  # -fexceptions
        'GCC_ENABLE_CPP_RTTI': 'YES',        # -frtti
      },
      'defines': ['ANTLR4CPP_STATIC'],
      'defines!': [
        '_HAS_EXCEPTIONS=0',
        'BUILDING_V8_SHARED=1',
      ],
      'include_dirs': [
        '../third_party/antlr4/runtime/Cpp/runtime/src',
        '../src/torque',
      ],
      # This is defined trough `configurations` for GYP+ninja compatibility
      'configurations': {
        'Debug': {
          'msvs_settings': {
            'VCCLCompilerTool': {
              'RuntimeTypeInfo': 'true',
              'ExceptionHandling': 1,
            },
          }
        },
        'Release': {
          'msvs_settings': {
            'VCCLCompilerTool': {
              'RuntimeTypeInfo': 'true',
              'ExceptionHandling': 1,
            },
          }
        },
      },
      'sources': [
        '../src/torque/TorqueBaseVisitor.cpp',
        '../src/torque/TorqueBaseVisitor.h',
        '../src/torque/TorqueLexer.cpp',
        '../src/torque/TorqueLexer.h',
        '../src/torque/TorqueParser.cpp',
        '../src/torque/TorqueParser.h',
        '../src/torque/TorqueVisitor.cpp',
        '../src/torque/TorqueVisitor.h',
        '../src/torque/ast-generator.cc',
        '../src/torque/ast-generator.h',
        '../src/torque/ast.h',
        '../src/torque/contextual.h',
        '../src/torque/declarable.cc',
        '../src/torque/declarable.h',
        '../src/torque/declaration-visitor.cc',
        '../src/torque/declaration-visitor.h',
        '../src/torque/declarations.cc',
        '../src/torque/declarations.h',
        '../src/torque/file-visitor.cc',
        '../src/torque/file-visitor.h',
        '../src/torque/global-context.h',
        '../src/torque/implementation-visitor.cc',
        '../src/torque/implementation-visitor.h',
        '../src/torque/scope.cc',
        '../src/torque/scope.h',
        '../src/torque/torque.cc',
        '../src/torque/type-oracle.h',
        '../src/torque/types.cc',
        '../src/torque/types.h',
        '../src/torque/utils.cc',
        '../src/torque/utils.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRErrorListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRErrorListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRErrorStrategy.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRErrorStrategy.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRFileStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRFileStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRInputStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ANTLRInputStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/BailErrorStrategy.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/BailErrorStrategy.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/BaseErrorListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/BaseErrorListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/BufferedTokenStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/BufferedTokenStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CharStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CharStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CommonToken.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CommonToken.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CommonTokenFactory.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CommonTokenFactory.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CommonTokenStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/CommonTokenStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ConsoleErrorListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ConsoleErrorListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/DefaultErrorStrategy.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/DefaultErrorStrategy.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/DiagnosticErrorListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/DiagnosticErrorListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Exceptions.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Exceptions.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/FailedPredicateException.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/FailedPredicateException.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/InputMismatchException.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/InputMismatchException.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/IntStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/IntStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/InterpreterRuleContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/InterpreterRuleContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Lexer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Lexer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/LexerInterpreter.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/LexerInterpreter.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/LexerNoViableAltException.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/LexerNoViableAltException.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ListTokenSource.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ListTokenSource.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/NoViableAltException.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/NoViableAltException.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Parser.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Parser.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ParserInterpreter.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ParserInterpreter.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ParserRuleContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ParserRuleContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ProxyErrorListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/ProxyErrorListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RecognitionException.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RecognitionException.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Recognizer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Recognizer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RuleContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RuleContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RuleContextWithAltNum.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RuleContextWithAltNum.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RuntimeMetaData.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/RuntimeMetaData.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Token.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Token.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenFactory.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenSource.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenSource.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenStreamRewriter.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/TokenStreamRewriter.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/UnbufferedCharStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/UnbufferedCharStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/UnbufferedTokenStream.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/UnbufferedTokenStream.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Vocabulary.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/Vocabulary.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/WritableToken.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/WritableToken.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/antlr4-common.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/antlr4-runtime.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATN.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATN.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNConfig.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNConfig.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNConfigSet.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNConfigSet.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNDeserializationOptions.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNDeserializationOptions.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNDeserializer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNDeserializer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNSerializer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNSerializer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNSimulator.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNSimulator.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ATNType.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/AbstractPredicateTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/AbstractPredicateTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ActionTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ActionTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/AmbiguityInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/AmbiguityInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ArrayPredictionContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ArrayPredictionContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/AtomTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/AtomTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BasicBlockStartState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BasicBlockStartState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BasicState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BasicState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BlockEndState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BlockEndState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BlockStartState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/BlockStartState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ContextSensitivityInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ContextSensitivityInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/DecisionEventInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/DecisionEventInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/DecisionInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/DecisionInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/DecisionState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/DecisionState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/EmptyPredictionContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/EmptyPredictionContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/EpsilonTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/EpsilonTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ErrorInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ErrorInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LL1Analyzer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LL1Analyzer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerATNConfig.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerATNConfig.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerATNSimulator.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerATNSimulator.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerActionExecutor.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerActionExecutor.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerActionType.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerChannelAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerChannelAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerCustomAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerCustomAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerIndexedCustomAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerIndexedCustomAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerModeAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerModeAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerMoreAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerMoreAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerPopModeAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerPopModeAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerPushModeAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerPushModeAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerSkipAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerSkipAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerTypeAction.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LexerTypeAction.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LookaheadEventInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LookaheadEventInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LoopEndState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/LoopEndState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/NotSetTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/NotSetTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/OrderedATNConfigSet.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/OrderedATNConfigSet.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ParseInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ParseInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ParserATNSimulator.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ParserATNSimulator.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PlusBlockStartState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PlusBlockStartState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PlusLoopbackState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PlusLoopbackState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PrecedencePredicateTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PrecedencePredicateTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredicateEvalInfo.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredicateEvalInfo.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredicateTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredicateTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredictionContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredictionContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredictionMode.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/PredictionMode.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ProfilingATNSimulator.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/ProfilingATNSimulator.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RangeTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RangeTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RuleStartState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RuleStartState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RuleStopState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RuleStopState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RuleTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/RuleTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/SemanticContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/SemanticContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/SetTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/SetTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/SingletonPredictionContext.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/SingletonPredictionContext.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/StarBlockStartState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/StarBlockStartState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/StarLoopEntryState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/StarLoopEntryState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/StarLoopbackState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/StarLoopbackState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/TokensStartState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/TokensStartState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/Transition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/Transition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/WildcardTransition.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/atn/WildcardTransition.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/DFA.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/DFA.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/DFASerializer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/DFASerializer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/DFAState.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/DFAState.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/LexerDFASerializer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/dfa/LexerDFASerializer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/InterpreterDataReader.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/InterpreterDataReader.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/Interval.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/Interval.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/IntervalSet.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/IntervalSet.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/MurmurHash.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/MurmurHash.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/Predicate.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/misc/Predicate.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/Any.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/Any.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/Arrays.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/Arrays.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/BitSet.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/CPPUtils.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/CPPUtils.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/Declarations.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/StringUtils.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/StringUtils.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/guid.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/support/guid.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/AbstractParseTreeVisitor.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ErrorNode.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ErrorNode.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ErrorNodeImpl.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ErrorNodeImpl.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/IterativeParseTreeWalker.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/IterativeParseTreeWalker.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTree.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTree.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeProperty.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeVisitor.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeVisitor.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeWalker.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/ParseTreeWalker.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/TerminalNode.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/TerminalNode.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/TerminalNodeImpl.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/TerminalNodeImpl.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/Trees.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/Trees.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/Chunk.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/Chunk.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/ParseTreeMatch.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/ParseTreeMatch.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/ParseTreePattern.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/ParseTreePattern.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/ParseTreePatternMatcher.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/ParseTreePatternMatcher.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/RuleTagToken.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/RuleTagToken.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/TagChunk.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/TagChunk.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/TextChunk.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/TextChunk.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/TokenTagToken.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/pattern/TokenTagToken.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPath.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPath.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathElement.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathLexer.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathLexer.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathLexerErrorListener.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathLexerErrorListener.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathRuleAnywhereElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathRuleAnywhereElement.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathRuleElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathRuleElement.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathTokenAnywhereElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathTokenAnywhereElement.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathTokenElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathTokenElement.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathWildcardAnywhereElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathWildcardAnywhereElement.h',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathWildcardElement.cpp',
        '../third_party/antlr4/runtime/Cpp/runtime/src/tree/xpath/XPathWildcardElement.h',
      ],
    },
    {
      'target_name': 'v8_torque',
      'type': 'none',
      'toolsets': ['host'],
      'dependencies': ['torque#host'],
      'direct_dependent_settings': {
        'include_dirs+': ['<(SHARED_INTERMEDIATE_DIR)'],
      },
      'actions': [
        {
          'action_name': 'run_torque',
          'inputs': [  # Order matters.
            '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)torque<(EXECUTABLE_SUFFIX)',
            '../src/builtins/base.tq',
            '../src/builtins/array.tq',
            '../src/builtins/typed-array.tq',
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtin-definitions-from-dsl.h',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-array-from-dsl-gen.cc',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-array-from-dsl-gen.h',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-base-from-dsl-gen.cc',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-base-from-dsl-gen.h',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-typed-array-from-dsl-gen.cc',
            '<(SHARED_INTERMEDIATE_DIR)/torque-generated/builtins-typed-array-from-dsl-gen.h',
          ],
          'action': ['<@(_inputs)', '-o', '<(SHARED_INTERMEDIATE_DIR)/torque-generated'],
        },
      ],
    },
    {
      'target_name': 'postmortem-metadata',
      'type': 'none',
      'variables': {
        'heapobject_files': [
            '../src/objects.h',
            '../src/objects-inl.h',
            '../src/objects/code-inl.h',
            '../src/objects/code.h',
            '../src/objects/data-handler.h',
            '../src/objects/data-handler-inl.h',
            '../src/objects/fixed-array-inl.h',
            '../src/objects/fixed-array.h',
            '../src/objects/js-array-inl.h',
            '../src/objects/js-array.h',
            '../src/objects/js-regexp-inl.h',
            '../src/objects/js-regexp.h',
            '../src/objects/js-regexp-string-iterator-inl.h',
            '../src/objects/js-regexp-string-iterator.h',
            '../src/objects/map.h',
            '../src/objects/map-inl.h',
            '../src/objects/scope-info.h',
            '../src/objects/script.h',
            '../src/objects/script-inl.h',
            '../src/objects/shared-function-info.h',
            '../src/objects/shared-function-info-inl.h',
            '../src/objects/string.h',
            '../src/objects/string-inl.h',
        ],
      },
      'actions': [
          {
            'action_name': 'gen-postmortem-metadata',
            'inputs': [
              '../tools//gen-postmortem-metadata.py',
              '<@(heapobject_files)',
            ],
            'outputs': [
              '<(SHARED_INTERMEDIATE_DIR)/debug-support.cc',
            ],
            'action': [
              'python',
              '../tools//gen-postmortem-metadata.py',
              '<@(_outputs)',
              '<@(heapobject_files)'
            ]
          }
        ]
    },
    {
      'target_name': 'mksnapshot',
      'type': 'executable',
      'dependencies': [
        'v8_base',
        'v8_init',
        'v8_libbase',
        'v8_libplatform',
        'v8_nosnapshot',
      ],
      'include_dirs+': [
        '..',
        '<(DEPTH)',
      ],
      'sources': [
        '../src/snapshot/mksnapshot.cc',
      ],
      'conditions': [
        ['v8_enable_i18n_support==1', {
          'dependencies': [
            '<(icu_gyp_path):icui18n',
            '<(icu_gyp_path):icuuc',
          ]
        }],
        ['want_separate_host_toolset==1', {
          'toolsets': ['host'],
        }, {
          'toolsets': ['target'],
        }],
      ],
    },
    {
      'target_name': 'v8_dump_build_config',
      'type': 'none',
      'variables': {
      },
      'actions': [
        {
          'action_name': 'v8_dump_build_config',
          'inputs': [
            '../tools//testrunner/utils/dump_build_config_gyp.py',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/v8_build_config.json',
          ],
          'action': [
            'python',
            '../tools//testrunner/utils/dump_build_config_gyp.py',
            '<(PRODUCT_DIR)/v8_build_config.json',
            'dcheck_always_on=<(dcheck_always_on)',
            'is_asan=<(asan)',
            'is_cfi=<(cfi_vptr)',
            'is_component_build=<(component)',
            'is_debug=<(CONFIGURATION_NAME)',
            # Not available in gyp.
            'is_gcov_coverage=0',
            'is_msan=<(msan)',
            'is_tsan=<(tsan)',
            # Not available in gyp.
            'is_ubsan_vptr=0',
            'target_cpu=<(target_arch)',
            'v8_enable_i18n_support=<(v8_enable_i18n_support)',
            'v8_enable_verify_predictable=<(v8_enable_verify_predictable)',
            'v8_target_cpu=<(v8_target_arch)',
            'v8_use_snapshot=<(v8_use_snapshot)',
          ],
          'conditions': [
            ['v8_target_arch=="mips" or v8_target_arch=="mipsel" \
              or v8_target_arch=="mips64" or v8_target_arch=="mips64el"', {
                'action':[
                  'mips_arch_variant=<(mips_arch_variant)',
                  'mips_use_msa=<(mips_use_msa)',
                ],
            }],
          ],
        },
      ],
    },
  ],
}
