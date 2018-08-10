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
        '../src/builtins/builtins-typedarray-gen.cc',
        '../src/builtins/builtins-typedarray-gen.h',
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
      'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
      ],
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
      'target_name': 'postmortem-metadata',
      'type': 'none',
      'variables': {
        'heapobject_files': [
            '../src/objects.h',
            '../src/objects-inl.h',
            '../src/objects/code.h',
            '../src/objects/code-inl.h',
            '../src/objects/fixed-array.h',
            '../src/objects/fixed-array-inl.h',
            '../src/objects/js-array.h',
            '../src/objects/js-array-inl.h',
            '../src/objects/js-regexp.h',
            '../src/objects/js-regexp-inl.h',
            '../src/objects/js-regexp-string-iterator-inl.h',
            '../src/objects/js-regexp-string-iterator.h',
            '../src/objects/map.h',
            '../src/objects/map-inl.h',
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
