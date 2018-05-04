# Copyright 2012 the V8 project authors. All rights reserved.
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
#       copyright notice, this list of conditions and the following
#       disclaimer in the documentation and/or other materials provided
#       with the distribution.
#     * Neither the name of Google Inc. nor the names of its
#       contributors may be used to endorse or promote products derived
#       from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
  'includes': ['../gypfiles/toolchain.gypi', '../gypfiles/features.gypi', 'inspector/inspector.gypi'],
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
            'v8dll-main.cc',
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
          '../include',
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
      'target_name': 'v8_builtins_setup',
      'type': 'static_library',
      'dependencies': [
        'v8_builtins_generators',
      ],
      'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
        '../include',
      ],
      'sources': [  ### gcmole(all) ###
        'setup-isolate-full.cc',
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
      'target_name': 'v8_builtins_generators',
      'type': 'static_library',
      'dependencies': [
        'v8_base',
      ],
       'variables': {
        'optimize': 'max',
      },
      'include_dirs+': [
        '..',
        '../include',
      ],
      'sources': [  ### gcmole(all) ###
        'builtins/builtins-arguments-gen.cc',
        'builtins/builtins-arguments-gen.h',
        'builtins/builtins-array-gen.cc',
        'builtins/builtins-async-function-gen.cc',
        'builtins/builtins-async-gen.cc',
        'builtins/builtins-async-gen.h',
        'builtins/builtins-async-generator-gen.cc',
        'builtins/builtins-async-iterator-gen.cc',
        'builtins/builtins-boolean-gen.cc',
        'builtins/builtins-call-gen.cc',
        'builtins/builtins-call-gen.h',
        'builtins/builtins-collections-gen.cc',
        'builtins/builtins-console-gen.cc',
        'builtins/builtins-constructor-gen.cc',
        'builtins/builtins-constructor-gen.h',
        'builtins/builtins-constructor.h',
        'builtins/builtins-conversion-gen.cc',
        'builtins/builtins-date-gen.cc',
        'builtins/builtins-debug-gen.cc',
        'builtins/builtins-forin-gen.cc',
        'builtins/builtins-forin-gen.h',
        'builtins/builtins-function-gen.cc',
        'builtins/builtins-generator-gen.cc',
        'builtins/builtins-global-gen.cc',
        'builtins/builtins-handler-gen.cc',
        'builtins/builtins-ic-gen.cc',
        'builtins/builtins-internal-gen.cc',
        'builtins/builtins-interpreter-gen.cc',
        'builtins/builtins-intl-gen.cc',
        'builtins/builtins-iterator-gen.h',
        'builtins/builtins-iterator-gen.cc',
        'builtins/builtins-math-gen.cc',
        'builtins/builtins-number-gen.cc',
        'builtins/builtins-object-gen.cc',
        'builtins/builtins-promise-gen.cc',
        'builtins/builtins-promise-gen.h',
        'builtins/builtins-proxy-gen.cc',
        'builtins/builtins-proxy-gen.h',
        'builtins/builtins-proxy-helpers-gen.cc',
        'builtins/builtins-proxy-helpers-gen.h',
        'builtins/builtins-regexp-gen.cc',
        'builtins/builtins-regexp-gen.h',
        'builtins/builtins-sharedarraybuffer-gen.cc',
        'builtins/builtins-string-gen.cc',
        'builtins/builtins-string-gen.h',
        'builtins/builtins-symbol-gen.cc',
        'builtins/builtins-typedarray-gen.cc',
        'builtins/builtins-utils-gen.h',
        'builtins/builtins-wasm-gen.cc',
        'builtins/setup-builtins-internal.cc',
        'ic/accessor-assembler.cc',
        'ic/accessor-assembler.h',
        'ic/binary-op-assembler.cc',
        'ic/binary-op-assembler.h',
        'ic/keyed-store-generic.cc',
        'ic/keyed-store-generic.h',
        'interpreter/interpreter-assembler.cc',
        'interpreter/interpreter-assembler.h',
        'interpreter/interpreter-generator.cc',
        'interpreter/interpreter-generator.h',
        'interpreter/interpreter-intrinsics-generator.cc',
        'interpreter/interpreter-intrinsics-generator.h',
        'interpreter/setup-interpreter-internal.cc',
        'interpreter/setup-interpreter.h',
      ],
      'conditions': [
        ['want_separate_host_toolset==1', {
          'toolsets': ['host', 'target'],
        }, {
          'toolsets': ['target'],
        }],
        ['v8_target_arch=="ia32"', {
          'sources': [  ### gcmole(arch:ia32) ###
            'builtins/ia32/builtins-ia32.cc',
          ],
        }],
        ['v8_target_arch=="x64"', {
          'sources': [  ### gcmole(arch:x64) ###
            'builtins/x64/builtins-x64.cc',
          ],
        }],
        ['v8_target_arch=="arm"', {
          'sources': [  ### gcmole(arch:arm) ###
            'builtins/arm/builtins-arm.cc',
          ],
        }],
        ['v8_target_arch=="arm64"', {
          'sources': [  ### gcmole(arch:arm64) ###
            'builtins/arm64/builtins-arm64.cc',
          ],
        }],
        ['v8_target_arch=="mips" or v8_target_arch=="mipsel"', {
          'sources': [  ### gcmole(arch:mipsel) ###
            'builtins/mips/builtins-mips.cc',
          ],
        }],
        ['v8_target_arch=="mips64" or v8_target_arch=="mips64el"', {
          'sources': [  ### gcmole(arch:mips64el) ###
            'builtins/mips64/builtins-mips64.cc',
          ],
        }],
        ['v8_target_arch=="ppc" or v8_target_arch=="ppc64"', {
          'sources': [  ### gcmole(arch:ppc) ###
            'builtins/ppc/builtins-ppc.cc',
          ],
        }],
        ['v8_target_arch=="s390" or v8_target_arch=="s390x"', {
          'sources': [  ### gcmole(arch:s390) ###
            'builtins/s390/builtins-s390.cc',
          ],
        }],
        ['v8_enable_i18n_support==0', {
          'sources!': [
            'builtins/builtins-intl-gen.cc',
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
            'setup-isolate-deserialize.cc',
            'snapshot/natives-external.cc',
            'snapshot/snapshot-external.cc',
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
      'sources': [  ### gcmole(all) ###
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
        'graal/callbacks.cc',
        'graal/graal_array.cc',
        'graal/graal_array_buffer.cc',
        'graal/graal_array_buffer_view.cc',
        'graal/graal_boolean.cc',
        'graal/graal_context.cc',
        'graal/graal_data.cc',
        'graal/graal_date.cc',
        'graal/graal_external.cc',
        'graal/graal_function.cc',
        'graal/graal_function_callback_arguments.cc',
        'graal/graal_function_callback_info.cc',
        'graal/graal_function_template.cc',
        'graal/graal_handle_content.cc',
        'graal/graal_isolate.cc',
        'graal/graal_map.cc',
        'graal/graal_message.cc',
        'graal/graal_missing_primitive.cc',
        'graal/graal_module.cc',
        'graal/graal_name.cc',
        'graal/graal_number.cc',
        'graal/graal_object.cc',
        'graal/graal_object_template.cc',
        'graal/graal_primitive.cc',
        'graal/graal_promise.cc',
        'graal/graal_property_callback_info.cc',
        'graal/graal_proxy.cc',
        'graal/graal_regexp.cc',
        'graal/graal_script.cc',
        'graal/graal_set.cc',
        'graal/graal_stack_frame.cc',
        'graal/graal_stack_trace.cc',
        'graal/graal_string.cc',
        'graal/graal_symbol.cc',
        'graal/graal_template.cc',
        'graal/graal_unbound_script.cc',
        'graal/graal_value.cc',
        'graal/v8.cc'
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
        '../include',
      ],
      'sources': [
        '../include/libplatform/libplatform.h',
        '../include/libplatform/libplatform-export.h',
        '../include/libplatform/v8-tracing.h',
        'libplatform/default-platform.h',
      ],
      'direct_dependent_settings': {
        'include_dirs': [
          '../include',
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
        '../include',
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
          '../include',
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
            'objects.h',
            'objects-inl.h',
            'objects/map.h',
            'objects/map-inl.h',
            'objects/script.h',
            'objects/script-inl.h',
            'objects/shared-function-info.h',
            'objects/shared-function-info-inl.h',
            'objects/string.h',
            'objects/string-inl.h',
        ],
      },
      'actions': [
          {
            'action_name': 'gen-postmortem-metadata',
            'inputs': [
              '../tools/gen-postmortem-metadata.py',
              '<@(heapobject_files)',
            ],
            'outputs': [
              '<(SHARED_INTERMEDIATE_DIR)/debug-support.cc',
            ],
            'action': [
              'python',
              '../tools/gen-postmortem-metadata.py',
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
        'v8_builtins_setup',
        'v8_libbase',
        'v8_libplatform',
        'v8_nosnapshot',
      ],
      'include_dirs+': [
        '..',
        '<(DEPTH)',
      ],
      'sources': [
        'snapshot/mksnapshot.cc',
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
            '../tools/testrunner/utils/dump_build_config_gyp.py',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/v8_build_config.json',
          ],
          'action': [
            'python',
            '../tools/testrunner/utils/dump_build_config_gyp.py',
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
            'v8_target_cpu=<(v8_target_arch)',
            'v8_use_snapshot=<(v8_use_snapshot)',
          ],
        },
      ],
    },
  ],
}
